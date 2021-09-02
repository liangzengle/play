package play.entity.cache.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import mu.KLogging
import play.entity.Entity
import play.entity.ImmutableEntity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
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

  private lateinit var cache: Cache<ID, CacheObj<ID, E>>

  private lateinit var initializer: EntityInitializer<E>

  private lateinit var expireEvaluator: ExpireEvaluator

  private val persistingEntities: ConcurrentMap<ID, E> = ConcurrentHashMap()

  private var evictShelter: ConcurrentMap<ID, E>? = null

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  private fun getCache(): Cache<ID, CacheObj<ID, E>> {
    ensureInitialized()
    return cache
  }

  override fun getCachedEntities(): Sequence<E> {
    return cache.asMap().values.asSequence().map { it.peekEntity() }.filterNotNull()
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
      builder.executor(executor)
      builder.initialCapacity(EntityCacheHelper.getInitialSizeOrDefault(entityClass, settings.initialSize))
      if (!isNeverExpire) {
        val duration =
          if ((cacheSpec?.expireAfterAccess ?: 0) > 0) Duration.ofSeconds(cacheSpec.expireAfterAccess.toLong())
          else settings.expireAfterAccess
        builder.expireAfterAccess(duration)
      }
      builder.evictionListener(CacheEvictListener())
      builder.removalListener(CacheRemovalListener())
      val cache: Cache<ID, CacheObj<ID, E>> = builder.build()
      val isLoadAllOnInit = cacheSpec?.loadAllOnInit ?: false
      if (isLoadAllOnInit) {
        logger.info { "Loading all [${entityClass.simpleName}]" }
        entityCacheLoader.loadAll(entityClass, cache) { c, e ->
          initializer.initialize(e)
          c.put(e.id(), CacheObj(e))
          c
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
        it.isNotEmpty() && it.peekEntity()?.isDeleted() != true
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
    return load(id, ::loadFromDB)
  }

  private fun loadFromDB(id: ID): E? {
    val f = entityCacheLoader.loadById(id, entityClass)
    try {
      val entity: E? = f.get(settings.loadTimeout).getOrNull()
      if (entity == null || entity.isDeleted()) {
        return null
      }
      initializer.initialize(entity)
      return entity
    } catch (e: Exception) {
      logger.error(e) { "Failed to load entity: ${entityClass.simpleName}($id)" }
      throw e
    }
  }

  private fun load(id: ID, fallback: (ID) -> E?): E? {
    val entityInShelter = evictShelter?.remove(id)
    if (entityInShelter != null) {
      return entityInShelter
    }
    val pendingPersist = persistingEntities.remove(id)
    if (pendingPersist != null) {
      return pendingPersist
    }
    return fallback(id)
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
    return computeIfAbsent(
      id,
      { k -> createEntity(k, creation) },
      { k -> load(k) ?: createEntity(k, creation) }
    ) ?: error("won't happen")
  }

  private fun createEntity(id: ID, creation: (ID) -> E): E {
    val entity = creation(id)
    initializer.initialize(entity)
    entityCacheWriter.insert(entity)
    return entity
  }

  override fun getCached(id: ID): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun getAll(ids: Iterable<ID>): List<E> {
    val result = arrayListOf<E>()
    val missing = arrayListOf<ID>()
    for (id in ids) {
      val entity = getOrNull(id)
      if (entity != null) {
        result.add(entity)
      } else {
        missing.add(id)
      }
    }
    if (missing.isEmpty()) {
      return result
    }
    val loaded = entityCacheLoader.loadAll(missing, entityClass).get(Duration.ofSeconds(5))
    for (entity in loaded) {
      if (entity.isDeleted()) {
        continue
      }
      val e = computeIfAbsent(entity.id(), null) { entity }
      if (e != null) {
        result.add(e)
      }
    }
    return result
  }

  override fun create(e: E): E {
    val entity = getOrCreate(e.id()) { e }
    if (entity !== e) {
      throw EntityExistsException(entityClass, e.id())
    }
    return entity
  }

  override fun delete(e: E) {
    delete(e.id())
  }

  override fun delete(id: ID) {
    getCache().asMap().compute(id) { k, v ->
      persistingEntities.remove(k)
      val entity = v?.peekEntity()
      if (entity != null) {
        entity.setDeleted()
        deleteFromDB(k)
      }
      CacheObj.empty()
    }
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
    val cache = getCache()
    val result = HashMap<ID, E>(cache.estimatedSize().toIntSaturated() + persistingEntities.size)
    for (entity in persistingEntities.values) {
      result[entity.id()] = entity
    }
    for (obj in cache.asMap().values) {
      val entity = obj.peekEntity()
      if (entity != null) {
        result[entity.id()] = entity
      }
    }
    return Json.stringify(result.values)
  }

  @Suppress("UNCHECKED_CAST")
  override fun persist(): Future<Unit> {
    val entities = getCache().asMap().values.asSequence()
      .filter { !(isImmutable && it.lastPersistTime != 0L) }
      .map { it.peekEntity() }
      .filterNotNull()
      .plus(persistingEntities.values.asSequence())
      .toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun initWithEmptyValue(id: ID) {
    val prev = getCache().asMap().putIfAbsent(id, CacheObj.empty())
    if (prev?.isNotEmpty() == false) {
      logger.warn { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  private fun computeIfAbsent(id: ID, loader: ((ID) -> E?)?): E? {
    return computeIfAbsent(id, null, loader)
  }

  private fun computeIfAbsent(
    id: ID,
    loadOnEmpty: ((ID) -> E)?,
    loadOnAbsent: ((ID) -> E?)?
  ): E? {
    val cache = getCache()
    var cacheObj = cache.getIfPresent(id)
    if (cacheObj != null) {
      val entity = cacheObj.accessEntity()
      if (entity != null) {
        return entity
      } else if (loadOnEmpty == null) {
        return null
      }
    }
    if (loadOnEmpty == null && loadOnAbsent == null) return null
    cacheObj = cache.asMap().compute(id) { k, v ->
      if (v == null) {
        if (loadOnAbsent == null) null else loadOnAbsent(k)?.let(::CacheObj) ?: CacheObj.empty()
      } else if (v.isEmpty() && loadOnEmpty != null) {
        CacheObj(loadOnEmpty(k))
      } else {
        v
      }
    }
    return cacheObj?.accessEntity()
  }

  private inner class CacheEvictListener : RemovalListener<ID, CacheObj<ID, E>> {
    override fun onRemoval(key: ID?, value: CacheObj<ID, E>?, cause: RemovalCause) {
      if (key == null) {
        return
      }
      val e = value?.peekEntity()
      if (e != null && cause.wasEvicted()) {
        // 过期入库
        persistOnExpired(e)
        val evictShelter = evictShelter
        check(evictShelter != null) { "`evictShelter` should not be null." }
        // 不允许过期的临时存放到evictShelter中
        if (!expireEvaluator.canExpire(e)) {
          evictShelter[key] = e
        }
      } else if (cause == RemovalCause.EXPLICIT) {
        deleteFromDB(key)
      }
    }
  }

  private inner class CacheRemovalListener : RemovalListener<ID, CacheObj<ID, E>> {
    override fun onRemoval(key: ID?, value: CacheObj<ID, E>?, cause: RemovalCause) {
      if (key == null) {
        return
      }
      // 将evictShelter中的对象重新放回缓存中
      if (cause.wasEvicted()) {
        val evictShelter = evictShelter
        if (evictShelter != null && evictShelter.containsKey(key)) {
          getCache().asMap().compute(key) { k, v ->
            val entity = evictShelter.remove(k)
            when {
              entity == null -> v
              v == null -> CacheObj(entity)
              else -> error("should not happen: $k")
            }
          }
        }
      }
    }
  }

  private fun deleteFromDB(id: ID) {
    entityCacheWriter.deleteById(id, entityClass).onFailure {
      // TODO what to do if failed
      logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
    }
  }

  /**
   * 缓存过期时回写数据库
   * @param e 过期的实体
   */
  private fun persistOnExpired(e: E) {
    if (e.isDeleted()) {
      return
    }
    val id = e.id()
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
            pending.isDeleted() -> CacheObj.empty()
            v == null -> CacheObj(pending)
            else -> error("should not happen")
          }
        }
        logger.error(result.getCause()) { "持久化失败: ${entityClass.simpleName}($id)" }
      }
    }
  }

  private class CacheObj<ID : Any, E : Entity<ID>>(private val entity: E?) {
    var lastPersistTime: Long = 0L

    @Volatile
    var lastAccessTime: Long = 0L

    fun peekEntity(): E? = entity

    fun isEmpty() = entity == null

    fun isNotEmpty() = entity != null

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
