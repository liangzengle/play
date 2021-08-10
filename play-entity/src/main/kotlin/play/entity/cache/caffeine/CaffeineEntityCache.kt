package play.entity.cache.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.google.common.collect.Sets
import mu.KLogging
import play.entity.Entity
import play.entity.ImmutableEntity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.collection.filterNotNull
import play.util.concurrent.Future
import play.util.control.getCause
import play.util.getOrNull
import play.util.json.Json
import play.util.primitive.toIntSaturated
import play.util.time.currentMillis
import play.util.toOptional
import play.util.unsafeCast
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.stream.Stream

internal class CaffeineEntityCache<ID : Any, E : Entity<ID>>(
  override val entityClass: Class<E>,
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: AbstractEntityCacheFactory.Settings,
  private val initializerProvider: EntityInitializerProvider
) : EntityCache<ID, E>, UnsafeEntityCacheOps<ID> {

  companion object : KLogging()

  @Volatile
  private var initialized = false

  private lateinit var initializer: EntityInitializer<E>

  private lateinit var cache: Cache<ID, CacheObj<ID, E>>

  private val persistingEntities: ConcurrentMap<ID, E> = ConcurrentHashMap()

  private lateinit var expireEvaluator: ExpireEvaluator

  @Volatile
  private var deleted: MutableSet<ID>? = null

  private var evictShelter: ConcurrentMap<ID, E>? = null

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  private fun getCache(): Cache<ID, CacheObj<ID, E>> {
    ensureInitialized()
    return cache
  }

  private fun ensureInitialized() {
    if (initialized) {
      return
    }
    synchronized(this) {
      if (initialized) {
        return
      }
      val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
      expireEvaluator = when (val expireEvaluator = cacheSpec?.expireEvaluator) {
        null -> DefaultExpireEvaluator
        DefaultExpireEvaluator::class -> DefaultExpireEvaluator
        NeverExpireEvaluator::class -> NeverExpireEvaluator
        else -> injector.getInstance(expireEvaluator.java)
      }
      val isNeverExpire = expireEvaluator == NeverExpireEvaluator
      evictShelter = if (isNeverExpire) null else ConcurrentHashMap()
      initializer = initializerProvider.get(entityClass)
      val builder: Caffeine<ID, CacheObj<ID, E>> = Caffeine.newBuilder().unsafeCast()
      builder.initialCapacity(EntityCacheHelper.getInitialSizeOrDefault(entityClass, settings.initialSize))
      if (!isNeverExpire) {
        val duration =
          if ((cacheSpec?.expireAfterAccess ?: 0) > 0) Duration.ofSeconds(cacheSpec.expireAfterAccess.toLong())
          else settings.expireAfterAccess
        builder.expireAfterAccess(duration)
      }
      builder.evictionListener(RemovalListener())
      val cache: Cache<ID, CacheObj<ID, E>> = builder.build()
      val isLoadAllOnInit = cacheSpec?.loadAllOnInit ?: false
      if (isLoadAllOnInit) {
        logger.info { "Loading all [${entityClass.simpleName}]" }
        entityCacheLoader.foreach(entityClass) { entity ->
          initializer.initialize(entity)
          cache.put(entity.id(), CacheObj(entity))
        }.await(Duration.ofSeconds(5))
        logger.info { "Loaded ${cache.estimatedSize()} [${entityClass.simpleName}] into cache." }
      }
      this.cache = cache
      if (!isImmutable) {
        scheduler.scheduleWithFixedDelay(
          settings.persistInterval,
          settings.persistInterval.dividedBy(2),
          executor,
          this::scheduledPersist
        )
      }
      initialized = true
    }
  }

  private fun scheduledPersist() {
    val now = currentMillis()
    val persistTimeThreshold = now - settings.persistInterval.toMillis()
    val entities = getCache().asMap().values.asSequence()
      .filter {
        it.hasEntity()
          && it.lastPersistTime < persistTimeThreshold
          && it.lastAccessTime > it.lastPersistTime
          && !(isImmutable && it.lastPersistTime != 0L)
      }
      .map {
        it.lastPersistTime = now
        it.peekEntity()!!
      }
      .toList()
    if (entities.isNotEmpty()) {
      entityCacheWriter.batchInsertOrUpdate(entities).onFailure { ex ->
        logger.error(ex) { "[${entityClass.simpleName}]批量更新失败" }
      }
    }
  }

  private fun load(id: ID): E? {
    val pendingPersist = persistingEntities.remove(id)
    if (pendingPersist != null) {
      return pendingPersist
    }
    val entityInShelter = evictShelter?.remove(id)
    if (entityInShelter != null) {
      return entityInShelter
    }
    val f = entityCacheLoader.findById(id, entityClass)
    try {
      val entity: E? = f.get(settings.loadTimeout).getOrNull()
      if (entity != null) {
        initializer.initialize(entity)
      }
      return entity
    } catch (e: Exception) {
      logger.error(e) { "Failed to load entity: ${entityClass.simpleName}($id)" }
      throw e
    }
  }

  override fun get(id: ID): Optional<E> {
    return getOrNull(id).toOptional()
  }

  override fun getOrNull(id: ID): E? {
    return computeIfAbsent(id, ::load)
  }

  override fun getOrThrow(id: ID): E {
    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    requireNotDeleted(id)
    return computeIfAbsent(
      id,
      true,
      { k -> createEntity(k, creation) },
      { k -> load(k) ?: createEntity(k, creation) }
    ) ?: error("won't happen")
  }

  private fun createEntity(id: ID, creation: (ID) -> E): E {
    val entity = creation(id)
    initializer.initialize(entity)
    entityCacheWriter.insert(entity)
    onCreate(entity)
    return entity
  }

  override fun getCached(id: ID): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun asSequence(): Sequence<E> {
    return getCache().asMap().values.asSequence().map { it.accessEntity() }.filterNotNull()
  }

  override fun asStream(): Stream<E> {
    return getCache().asMap().values.stream().map { it.accessEntity() }.filterNotNull()
  }

  override fun create(e: E): E {
    val entity = getOrCreate(e.id()) { e }
    if (entity !== e) {
      throw EntityExistsException(entityClass, e.id())
    }
    return entity
  }

  override fun delete(e: E) {
    deleteById(e.id())
  }

  override fun deleteById(id: ID) {
    addDeleteRecord(id)
    persistingEntities.remove(id)
    getCache().invalidate(id)
    entityCacheWriter.deleteById(id, entityClass).onFailure {
      // TODO what to do if failed
      logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
    }
  }

  override fun flush(id: ID) {
    val cached = getCached(id).getOrNull()
      ?: throw IllegalStateException("${entityClass.simpleName}($id)保存失败，缓存不存在")
    entityCacheWriter.update(cached)
  }

  override fun size(): Int {
    return getCache().estimatedSize().toIntSaturated()
  }

  override fun isCached(id: ID): Boolean {
    return getCache().asMap().containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return getCache().asMap().isEmpty()
  }

  override fun dump(): String {
    val entities = (asSequence() + persistingEntities.values.asSequence()).toCollection(LinkedList())
    return Json.stringify(entities)
  }

  @Suppress("UNCHECKED_CAST")
  override fun flush(): Future<Unit> {
    val entities = getCache().asMap().values.asSequence()
      .apply { if (isImmutable) filter { it.lastPersistTime == 0L } }
      .map { it.peekEntity() }
      .filterNotNull()
      .plus(persistingEntities.values.asSequence())
      .toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun initWithEmptyValue(id: ID) {
    val prev = getCache().asMap().putIfAbsent(id, CacheObj.empty())
    if (prev?.hasEntity() == false) {
      logger.warn { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  override fun deleteUnprotected(id: ID) {
    getCache().asMap().compute(id) { key, _ ->
      entityCacheWriter.deleteById(key, entityClass).onComplete {
        if (it.isFailure) {
          addDeleteRecord(key)
          logger.error(it.getCause()) { "Unsafe delete failed: ${entityClass.simpleName}($key)" }
        } else {
          logger.info { "Unsafe delete: ${entityClass.simpleName}($key)" }
        }
      }
      CacheObj.empty()
    }
  }

  private fun computeIfAbsent(id: ID, loader: ((ID) -> E?)?): E? {
    return computeIfAbsent(id, false, null, loader)
  }

  private fun computeIfAbsent(
    id: ID,
    createIfAbsent: Boolean,
    loadOnEmpty: ((ID) -> E)?,
    loadOnAbsent: ((ID) -> E?)?
  ): E? {
    val cache = getCache()
    var cacheObj = cache.getIfPresent(id)
    if (cacheObj != null) {
      val entity = cacheObj.accessEntity()
      if (entity != null) {
        return entity
      } else if (!createIfAbsent) {
        return null
      }
    }
    if (loadOnAbsent == null) return null
    cacheObj = cache.asMap().compute(id) { k, v ->
      when {
        isDeleted(k) -> null
        v?.peekEntity() != null -> v
        // equals: v != null && v.entity == null && loadOnEmpty != null
        v != null && loadOnEmpty != null -> CacheObj(loadOnEmpty(k))
        else -> loadOnAbsent(k)?.let(::CacheObj) ?: CacheObj.empty()
      }
    }
    return cacheObj?.accessEntity()
  }

  private fun isDeleted(id: ID) = deleted?.contains(id) ?: false

  private inner class RemovalListener :
    com.github.benmanes.caffeine.cache.RemovalListener<ID, CacheObj<ID, E>> {
    override fun onRemoval(key: ID?, value: CacheObj<ID, E>?, cause: RemovalCause) {
      if (key == null) {
        return
      }
      val e = value?.peekEntity()
      if (e != null && cause.wasEvicted()) {
        val evictShelter = evictShelter
        check(evictShelter != null) { "`evictShelter` should not be null." }
        // 不允许过期则重新放回cache中
        if (!expireEvaluator.canExpire(e)) {
          evictShelter[key] = e
          // 异步放回
          executor.execute {
            getCache().asMap().compute(key) { k, v ->
              val entity = evictShelter.remove(k)
              when {
                entity == null -> v
                v == null -> CacheObj(entity)
                else -> error("should not happen")
              }
            }
          }
        }
        persistOnExpired(e)
      } else if (cause == RemovalCause.EXPLICIT) {
        recordAndDelete(key)
      }
    }
  }

  private fun recordAndDelete(id: ID) {
    addDeleteRecord(id)
    entityCacheWriter.deleteById(id, entityClass).onFailure {
      // TODO what to do if failed
      logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
    }
  }

  private fun addDeleteRecord(id: ID): Boolean {
    if (deleted == null) {
      synchronized(this) {
        if (deleted == null) {
          deleted = Sets.newConcurrentHashSet()
        }
      }
    }
    return deleted!!.add(id)
  }

  /**
   * 缓存过期时回写数据库
   * @param e 过期的实体
   */
  private fun persistOnExpired(e: E) {
    val id = e.id()
    requireNotDeleted(id)
    persistingEntities[id] = e
    entityCacheWriter.insertOrUpdate(e).onComplete { result ->
      if (result.isSuccess) {
        persistingEntities.remove(id)
      } else {
        // 写数据库失败，重新放回缓存
        getCache().asMap().compute(id) { k, v ->
          val pending = persistingEntities.remove(k)
          when {
            pending == null -> v
            v == null -> CacheObj(pending)
            else -> error("should not happen")
          }
        }
        logger.error(result.getCause()) { "持久化失败: ${entityClass.simpleName}($id)" }
      }
    }
  }

  private fun requireNotDeleted(id: ID) {
    if (isDeleted(id)) throw IllegalStateException("实体已被删除: ${entityClass.simpleName}($id)")
  }

  private class CacheObj<ID : Any, E : Entity<ID>>(private val entity: E?) {
    var lastPersistTime: Long = 0L

    @Volatile
    var lastAccessTime: Long = 0L

    fun peekEntity(): E? = entity

    fun hasEntity() = entity != null

    fun accessEntity(): E? {
      lastAccessTime = currentMillis()
      return entity
    }

    override fun toString(): String {
      return entity?.toString() ?: "CacheObj(<empty>)"
    }

    companion object {
      private val Empty = CacheObj<Any, Entity<Any>>(null)
      fun <ID : Any, E : Entity<ID>> empty() = Empty.unsafeCast<CacheObj<ID, E>>()
    }
  }
}
