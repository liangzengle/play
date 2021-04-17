package play.entity.cache.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.CacheWriter
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.google.common.collect.Sets
import com.google.inject.Injector
import play.entity.Entity
import play.entity.ImmutableEntity
import play.entity.cache.*
import play.scheduling.Scheduler
import play.util.collection.filterNotNull
import play.util.concurrent.Future
import play.util.control.getCause
import play.util.getOrNull
import play.util.json.Json
import play.util.logging.getLogger
import play.util.primitive.toIntSaturated
import play.util.time.currentMillis
import play.util.toOptional
import play.util.unsafeCast
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.stream.Stream
import kotlin.time.minutes

internal class CaffeineEntityCache<ID : Any, E : Entity<ID>>(
  override val entityClass: Class<E>,
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  injector: Injector,
  scheduler: Scheduler,
  private val executor: Executor,
  private val settings: AbstractEntityCacheFactory.Settings,
  private val initializer: EntityInitializer<E>
) : EntityCache<ID, E>, UnsafeEntityCacheOps<ID> {

  private val cache: Cache<ID, CacheObj<ID, E>>
  private val persistingEntities: ConcurrentMap<ID, E> = ConcurrentHashMap()

  private val expireEvaluator: ExpireEvaluator

  @Volatile
  private var deleted: MutableSet<ID>? = null

  private val evictShelter: ConcurrentMap<ID, E>?

  companion object {
    @JvmStatic
    private val logger = getLogger()
    private val DeleteNoRecord = CacheObj<Any, Entity<Any>>(null)
  }

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  init {
    val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
    expireEvaluator = if (cacheSpec != null && cacheSpec.expireEvaluator != DefaultExpireEvaluator::class) {
      injector.getInstance(cacheSpec.expireEvaluator.java)
    } else DefaultExpireEvaluator

    val isNeverExpire = expireEvaluator is NeverExpireEvaluator
    evictShelter = if (isNeverExpire) null else ConcurrentHashMap()

    if (isNeverExpire) {
      logger.info { "[${entityClass.simpleName}]缓存将永不过期" }
    }

    val builder: Caffeine<ID, CacheObj<ID, E>> = Caffeine.newBuilder().unsafeCast()
    builder.initialCapacity(EntityCacheHelper.getInitialSizeOrDefault(entityClass, settings.initialSize))
    if (!isNeverExpire) {
      builder.expireAfterAccess(settings.expireAfterAccess)
    }
    builder.writer(Writer())
    val cache: Cache<ID, CacheObj<ID, E>> = builder.build()
    val isLoadAllOnInit = cacheSpec?.loadAllOnInit ?: false
    if (isLoadAllOnInit) {
      logger.info { "Loading all [${entityClass.simpleName}]" }
      entityCacheLoader.foreach(entityClass) { entity ->
        initializer.initialize(entity)
        cache.put(entity.id(), CacheObj(entity))
      }.await(1.minutes)
      logger.info { "Loaded ${cache.estimatedSize()} [${entityClass.simpleName}] into cache." }
    }
    this.cache = cache

    if (!isImmutable) {
      scheduler.scheduleAtFixedRate(
        settings.persistInterval,
        settings.persistInterval.dividedBy(2),
        executor,
        this::scheduledPersist
      )
    } else {
      logger.info { "[${entityClass.simpleName}]标记为不可变实体，将不会执行定时入库" }
    }
  }

  private fun scheduledPersist() {
    val now = currentMillis()
    val persistTimeThreshold = now - settings.persistInterval.toMillis()
    val entities = cache.asMap().values.asSequence()
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
      { k -> runCreation(k, creation) },
      { k -> load(k) ?: runCreation(k, creation) }
    ) ?: error("won't happen")
  }

  private fun runCreation(id: ID, creation: (ID) -> E): E {
    val entity = creation(id)
    initializer.initialize(entity)
    entityCacheWriter.insert(entity)
    return entity
  }

  override fun getCached(id: ID): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun asSequence(): Sequence<E> {
    return cache.asMap().values.asSequence().map { it.accessEntity() }.filterNotNull()
  }

  override fun asStream(): Stream<E> {
    return cache.asMap().values.stream().map { it.accessEntity() }.filterNotNull()
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
    persistingEntities.remove(id)
    val cached = getCached(id).isEmpty
    cache.invalidate(id)
    // 允许删除不在缓存的实体
    if (!cached) {
      recordAndDelete(id)
    }
  }

  override fun flush(id: ID) {
    val cached = getCached(id).getOrNull()
      ?: throw IllegalStateException("${entityClass.simpleName}($id)保存失败，缓存不存在")
    entityCacheWriter.update(cached)
  }

  override fun size(): Int {
    return cache.estimatedSize().toIntSaturated()
  }

  override fun isCached(id: ID): Boolean {
    return cache.asMap().containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return cache.asMap().isEmpty()
  }

  override fun dump(): String {
    val entities = (asSequence() + persistingEntities.values.asSequence()).toCollection(LinkedList())
    return Json.stringify(entities)
  }

  @Suppress("UNCHECKED_CAST")
  override fun flush(): Future<Unit> {
    val entities = cache.asMap().values.asSequence()
      .apply { if (isImmutable) filter { it.lastPersistTime == 0L } }
      .map { it.peekEntity() }
      .filterNotNull()
      .plus(persistingEntities.values.asSequence())
      .toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun initWithEmptyValue(id: ID) {
    val prev = cache.asMap().putIfAbsent(id, CacheObj.empty())
    if (prev?.hasEntity() == false) {
      logger.warn { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  override fun deleteUnprotected(id: ID) {
    // delete operation handled by Writer.write
    cache.asMap().putIfAbsent(id, DeleteNoRecord.unsafeCast())
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

  private inner class Writer : CacheWriter<ID, CacheObj<ID, E>> {
    override fun write(key: ID, value: CacheObj<ID, E>) {
      if (value === DeleteNoRecord) {
        entityCacheWriter.deleteById(key, entityClass).onComplete {
          if (it.isFailure) {
            // TODO what to do if failed
            logger.error(it.getCause()) { "Unsafe delete failed: ${entityClass.simpleName}($key)" }
          } else {
            logger.info { "Unsafe delete: ${entityClass.simpleName}($key)" }
          }
        }
      }
    }

    override fun delete(id: ID, cacheObj: CacheObj<ID, E>?, cause: RemovalCause) {
      val e = cacheObj?.peekEntity()
      if (!cause.wasEvicted()) {
        recordAndDelete(id)
      } else if (e != null) {
        check(evictShelter != null) { "`evictShelter` should not be null." }
        // 不允许过期则重新放回cache中
        if (!expireEvaluator.canExpire(e)) {
          evictShelter[id] = e
          // 异步放回
          executor.execute {
            cache.asMap().compute(id) { k, v ->
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
      }
    }
  }

  private fun recordAndDelete(id: ID) {
    if (addDeleteRecord(id)) {
      entityCacheWriter.deleteById(id, entityClass).onFailure {
        // TODO what to do if failed
        logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
      }
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
        cache.asMap().compute(id) { k, v ->
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
