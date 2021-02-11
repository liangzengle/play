package play.db.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.CacheWriter
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.google.common.collect.Sets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.NoSuchElementException
import kotlin.time.minutes
import kotlin.time.seconds
import play.Configuration
import play.Log
import play.db.DbExecutor
import play.db.Entity
import play.db.PersistService
import play.db.QueryService
import play.getLogger
import play.inject.Injector
import play.util.collection.filterNotNull
import play.util.concurrent.CommonPool
import play.util.concurrent.Future
import play.util.control.getCause
import play.util.getOrNull
import play.util.json.Json
import play.util.primitive.toIntSaturated
import play.util.scheduling.Scheduler
import play.util.time.currentMillis
import play.util.toOptional
import play.util.unsafeCast

/**
 * Factory for creating CaffeineEntityCache
 * @author LiangZengle
 */
@Singleton
class CaffeineEntityCacheFactory @Inject constructor(
  private val persistService: PersistService,
  private val queryService: QueryService,
  private val injector: Injector,
  private val scheduler: Scheduler,
  private val executor: DbExecutor,
  @Named("cache") conf: Configuration
) : AbstractEntityCacheFactory(conf) {

  override fun <ID : Any, E : Entity<ID>> create(entityClass: Class<E>): EntityCache<ID, E> {
    checkEntityClass(entityClass)
    return CaffeineEntityCache(
      entityClass,
      persistService,
      queryService,
      injector,
      scheduler,
      executor,
      config
    )
  }
}

internal class CaffeineEntityCache<ID : Any, E : Entity<ID>>(
  override val entityClass: Class<E>,
  private val persistService: PersistService,
  private val queryService: QueryService,
  injector: Injector,
  scheduler: Scheduler,
  executor: DbExecutor,
  private val conf: AbstractEntityCacheFactory.Config
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
    private val Deleted = CacheObj<Any, Entity<Any>>(null)
  }

  init {
    val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
    expireEvaluator = if (cacheSpec != null && cacheSpec.expireEvaluator != DefaultExpireEvaluator::class) {
      injector.getInstance(cacheSpec.expireEvaluator.java)
    } else {
      injector.getInstance(DefaultExpireEvaluator::class.java)
    }
    evictShelter = if (expireEvaluator !is NeverExpireEvaluator) ConcurrentHashMap() else null

    val builder: Caffeine<ID, CacheObj<ID, E>> = Caffeine.newBuilder().unsafeCast()
    builder.initialCapacity(EntityCacheHelper.getInitialSizeOrDefault(entityClass, conf.initialSize))
    if (expireEvaluator !is NeverExpireEvaluator) {
      builder.expireAfterAccess(conf.expireAfterAccess)
    }
    builder.writer(Writer())
    val cache: Cache<ID, CacheObj<ID, E>> = builder.build()

    val persistStrategy = cacheSpec?.persistStrategy ?: CacheSpec.PersistStrategy.Scheduled
    if (persistStrategy == CacheSpec.PersistStrategy.Scheduled) {
      scheduler.scheduleAtFixedRate(
        conf.persistInterval,
        conf.persistInterval.dividedBy(2),
        executor,
        createPersistTask()
      )
    } else {
      Log.info { "[${entityClass.simpleName}] using [$persistStrategy] persist strategy." }
    }

    val isLoadAllOnInit = cacheSpec?.loadAllOnInit ?: false
    cache.asMap().entries
    if (isLoadAllOnInit) {
      Log.info { "loading all [${entityClass.simpleName}]" }
      queryService.foreach(entityClass) {
        it.postLoad()
        cache.put(it.id(), CacheObj(it))
      }.get(1.minutes)
      Log.info { "loaded ${cache.estimatedSize()} [${entityClass.simpleName}]" }
    }
    this.cache = cache
  }

  private fun createPersistTask(): () -> Unit = {
    val now = currentMillis()
    val persistTimeThreshold = now - conf.persistInterval.toMillis()
    val entities = cache.asMap().values.asSequence()
      .filter { it.hasEntity() && it.lastPersistTime < persistTimeThreshold }
      .map {
        it.lastPersistTime = now
        it.entity!!
      }
      .toList()
    if (entities.isNotEmpty()) {
      persistService.batchInsertOrUpdate(entities)
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
    val f = queryService.findById(id, entityClass)
    try {
      val entity: E? = f.get(5.seconds).getOrNull()
      entity?.postLoad()
      return entity
    } catch (e: Exception) {
      logger.error(e) { "查询数据库失败: ${entityClass.simpleName}($id)" }
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
      { k ->
        val entity = creation(k)
        entity.postLoad()
        persistService.insert(entity)
        entity
      },
      { k ->
        var entity = load(k)
        if (entity == null) {
          entity = creation(k)
          entity.postLoad()
          persistService.insert(entity)
        }
        entity
      }
    ) ?: error("won't happen")
  }

  override fun getCached(id: ID): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun asSequence(): Sequence<E> {
    return cache.asMap().values.asSequence().map { it.entity }.filterNotNull()
  }

  override fun asStream(): Stream<E> {
    return cache.asMap().values.stream().map { it.entity }.filterNotNull()
  }

  override fun create(e: E): E {
    val entity = getOrCreate(e.id()) { e }
    if (entity !== e) {
      throw EntityExistsException(entityClass, e.id())
    }
    return entity
  }

  override fun remove(e: E) {
    cache.invalidate(e.id())
  }

  override fun removeById(id: ID) {
    cache.invalidate(id)
  }

  override fun save(e: E) {
    val opt = getCached(e.id())
    if (opt.isPresent && opt.get() !== e) {
      throw IllegalStateException("${entityClass.simpleName}(${e.id()})保存失败，与缓存中的对象不一致")
    } else {
      persistService.update(e)
    }
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

  override fun dump(): String = Json.stringify(entitySequence().toList())

  @Suppress("UNCHECKED_CAST")
  override fun flush(): Future<Unit> {
    return persistService.batchInsertOrUpdate(entitySequence().toList()) as Future<Unit>
  }

  override fun initWithEmptyValue(id: ID) {
    val prev = cache.asMap().putIfAbsent(id, CacheObj.empty())
    if (prev?.hasEntity() == false) {
      logger.warn { "initWithEmptyValue失败, Entity已经存在: $prev" }
    }
  }

  override fun deleteUnprotected(id: ID) {
    cache.asMap().putIfAbsent(id, Deleted.unsafeCast())
  }

  private fun entitySequence(): Sequence<E> {
    return asSequence() + persistingEntities.values.asSequence()
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
      if (cacheObj.entity != null) {
        return cacheObj.entity
      } else if (!createIfAbsent) {
        return null
      }
    }
    if (loadOnAbsent === null) return null
    cacheObj = cache.asMap().compute(id) { k, v ->
      when {
        isDeleted(k) -> null
        v?.entity != null -> v
        // v != null && v.entity == null && loadOnEmpty !== null
        v != null && loadOnEmpty !== null -> CacheObj(loadOnEmpty(k))
        else -> loadOnAbsent(k)?.let(::CacheObj) ?: CacheObj.empty()
      }
    }
    return cacheObj?.entity
  }

  private fun isDeleted(id: ID) = deleted?.contains(id) ?: false

  private inner class Writer : CacheWriter<ID, CacheObj<ID, E>> {
    override fun write(key: ID, value: CacheObj<ID, E>) {
      if (value === Deleted) {
        persistService.deleteById(key, entityClass).onComplete {
          if (it.isFailure) {
            logger.error(it.getCause()) { "${entityClass.simpleName}($key)删除失败" }
          } else {
            logger.info { "Unprotected delete: ${entityClass.simpleName}($key)" }
          }
        }
      }
    }

    override fun delete(id: ID, value: CacheObj<ID, E>?, cause: RemovalCause) {
      val e = value?.entity
      if (!cause.wasEvicted()) {
        if (delete(id)) {
          persistService.deleteById(id, entityClass).onFailure {
            logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
          }
        }
      } else if (e != null) {
        if (evictShelter != null && !expireEvaluator.canExpire(e)) {
          evictShelter[id] = e
          // TODO think over
          CommonPool.execute {
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
        writeToDB(e)
      }
    }
  }

  private fun delete(id: ID): Boolean {
    if (deleted == null) {
      synchronized(this) {
        if (deleted == null) {
          deleted = Sets.newConcurrentHashSet()
        }
      }
    }
    return deleted!!.add(id)
  }

  private fun writeToDB(e: E) {
    val id: ID = e.id()
    requireNotDeleted(id)
    persistingEntities[id] = e
    persistService.insertOrUpdate(e).onComplete { result ->
      if (result.isSuccess) {
        persistingEntities.remove(id)
      } else {
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

  private class CacheObj<ID : Any, E : Entity<ID>>(val entity: E?) {
    var lastPersistTime: Long = 0L

    fun hasEntity() = entity != null

    override fun toString(): String {
      return entity?.toString() ?: "Empty"
    }

    companion object {
      private val Empty = CacheObj<Any, Entity<Any>>(null)
      fun <ID : Any, E : Entity<ID>> empty() = Empty.unsafeCast<CacheObj<ID, E>>()
    }
  }
}
