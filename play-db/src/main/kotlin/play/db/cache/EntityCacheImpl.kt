package play.db.cache

import com.google.common.collect.Sets
import io.vavr.concurrent.Future
import io.vavr.control.Option
import io.vavr.kotlin.option
import play.Log
import play.db.*
import play.getLogger
import play.inject.Injector
import play.util.json.Json
import play.util.scheduling.Scheduler
import play.util.time.currentMillis
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import javax.annotation.Nullable

internal class EntityCacheImpl<ID : Any, E : Entity<ID>>(
  private val entityClass: Class<E>,
  private val persistService: PersistService,
  private val queryService: QueryService,
  private val injector: Injector,
  scheduler: Scheduler,
  executor: DbExecutor,
  private val conf: DefaultEntityCacheFactory.Config,
  private val entityProcessor: EntityProcessor<E>
) : EntityCache<ID, E> {

  companion object {
    @JvmStatic
    private val logger = getLogger()

    @JvmStatic
    private val NOOP: (Any) -> Any? = { null }
  }

  private val cache: ConcurrentMap<ID, CacheObj<ID, E>> = ConcurrentHashMap(initialSize())
  private val pendingPersistCache: ConcurrentMap<ID, E> = ConcurrentHashMap()

  @Volatile
  private var deleted: MutableSet<ID>? = null

  private val expireEvaluator: ExpireEvaluator = createExpireEvaluator()

  private fun createExpireEvaluator(): ExpireEvaluator {
    val annotation = entityClass.getAnnotation(CacheSpec::class.java)
    return if (annotation != null && annotation.expireEvaluator != DefaultExpireEvaluator::class) {
      injector.instanceOf(annotation.expireEvaluator.java)
    } else {
      injector.instanceOf(DefaultExpireEvaluator::class.java)
    }
  }

  private fun initialSize(): Int {
    return entityClass.getAnnotation(CacheSpec::class.java)?.initialSize?.let {
      when (it.first()) {
        'x', 'X' -> it.substring(1).toInt() * conf.initialSize
        else -> it.toInt()
      }
    } ?: conf.initialSize
  }


  init {
    val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
    if (cacheSpec?.loadAllOnInit == true) {
      Log.info { "loading all [${entityClass.simpleName}]" }
      val f = queryService.listAll(entityClass).await(1, TimeUnit.MINUTES)
      if (f.isFailure) {
        throw EntityCacheInitializeException(entityClass, f.cause.get())
      }
      val entities = f.get()
      entities.forEach { entity -> cache[entity.id()] = CacheObj(entity, currentMillis()) }
      Log.info { "loaded ${entities.size} [${entityClass.simpleName}]" }
    }
    val persistType = cacheSpec?.persistType ?: CacheSpec.PersistType.Scheduled
    if (persistType == CacheSpec.PersistType.Scheduled) {
      scheduler.scheduleAtFixedRate(
        conf.persistInterval,
        conf.persistInterval.dividedBy(2),
        executor,
        createPersistTask()
      )
    } else {
      Log.info { "[${entityClass.simpleName}] using [$persistType] persist type." }
    }

    if (expireEvaluator !is NeverExpireEvaluator) {
      scheduler.scheduleAtFixedRate(
        conf.expireAfterAccess,
        conf.expireAfterAccess.dividedBy(2),
        executor,
        createExpirationTask()
      )
    } else {
      Log.info { "[${entityClass.simpleName}] will never expire." }
    }
  }

  private fun createPersistTask(): () -> Unit {
    return {
      val now = currentMillis()
      val persistTimeThreshold = now - conf.persistInterval.toMillis()
      val entities = cache.values.asSequence()
        .filter { it.hasEntity() && it.lastPersistTime < persistTimeThreshold }
        .map {
          it.lastPersistTime = now
          it.getEntitySilently()!!
        }
        .toList()
      if (entities.isNotEmpty()) {
        persistService.batchInsertOrUpdate(entities)
      }
    }
  }

  private fun createExpirationTask(): () -> Unit {
    return {
      val accessTimeThreshold = currentMillis() - conf.expireAfterAccess.toMillis()
      cache.values.asSequence()
        .filter { it.hasEntity() && it.accessTime <= accessTimeThreshold && expireEvaluator.canExpire(it.getEntitySilently()!!) }
        .forEach {
          cache.computeIfPresent(it.getId()) { _, v ->
            if (v.accessTime > accessTimeThreshold) {
              v
            } else {
              v.setExpired()
              if (v.hasEntity()) {
                writeToDB(v.getEntitySilently()!!)
              }
              null
            }
          }
        }
    }
  }

  private fun writeToDB(e: E) {
    val id: ID = e.id()
    pendingPersistCache[id] = e
    persistService.update(e).onComplete { result ->
      if (result.isSuccess) {
        pendingPersistCache.remove(id)
      } else {
        // restore into cache
        cache.computeIfAbsent(id) {
          val pending = pendingPersistCache.remove(id)
          if (pending == null) null
          else CacheObj(pending, currentMillis())
        }
        // TODO
        logger.error(result.cause) { "持久化失败: ${entityClass.simpleName}($id)" }
      }
    }
  }

  override fun entityClass(): Class<E> {
    return entityClass
  }

  override fun get(id: ID): Option<E> {
    return getOrNull(id).option()
  }

  @Nullable
  override fun getOrNull(id: ID): E? {
    return computeIfAbsent(id, dbLoader)
  }

  override fun getOrThrow(id: ID): E {
    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    requireNotDeleted(id)
    return computeIfAbsent(id, true) {
      var entity = dbLoader(it)
      if (entity == null) {
        entity = creation(it)
        entityProcessor.postLoad(entity)
        persistService.insert(entity)
      }
      entity
    }!!
  }

  @Suppress("UNCHECKED_CAST")
  override fun getCached(id: ID): Option<E> {
    return computeIfAbsent(id, NOOP as (ID) -> E?).option()
  }

  override fun asSequence(): Sequence<E> {
    return cache.values.asSequence().map { it.getEntitySilently() }.filterNotNull()
  }

  override fun create(e: E): E {
    requireNotDeleted(e.id())
    val that = getOrCreate(e.id()) { e }
    if (e !== that) {
      throw EntityExistsException(e.javaClass, e.id())
    }
    return e
  }

  override fun remove(e: E) {
    removeById(e.id())
  }

  override fun removeById(id: ID) {
    cache.compute(id) { k, _ ->
      if (delete(k)) {
        persistService.deleteById(k, entityClass)
      }
      null
    }
  }

  override fun save(e: E) {
    persistService.update(e)
  }

  override fun size(): Int {
    return cache.size
  }

  override fun isCached(id: ID): Boolean {
    return cache.containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return cache.isEmpty()

  }

  private val dbLoader: (ID) -> E? = { id ->
    val pendingPersist = pendingPersistCache.remove(id)
    if (pendingPersist != null) {
      pendingPersist
    } else {
      val f = queryService.findById(id, entityClass)
      f.await(5, TimeUnit.SECONDS)
      val queryResult = f.value.get()
      if (queryResult.isFailure) {
        logger.error(queryResult.cause) { "查询数据库失败: ${entityClass.simpleName}($id)" }
        throw queryResult.cause
      } else {
        val entity = queryResult.get().orNull
        if (entity != null) {
          entityProcessor.postLoad(entity)
        }
        entity
      }
    }
  }

  private fun computeIfAbsent(id: ID, loader: (ID) -> E?): E? {
    return computeIfAbsent(id, false, loader)
  }

  private fun computeIfAbsent(id: ID, createIfAbsent: Boolean, loader: (ID) -> E?): E? {
    var cacheObj = cache[id]
    if (cacheObj != null) {
      if (cacheObj.isEmpty() && !createIfAbsent) {
        return null
      }
      val entity = cacheObj.getEntity()
      if (entity != null && !cacheObj.isExpired()) {
        return entity
      }
    }
    if (loader === NOOP) return null

    cacheObj = cache.compute(id) { k, obj ->
      if (isDeleted(k)) {
        null
      } else if (obj == null || obj.isEmpty()) {
        val v = loader(k)
        if (v == null) CacheObj.empty() else CacheObj(v, currentMillis())
      } else {
        obj.accessTime = currentMillis()
        obj
      }
    }
    return cacheObj?.getEntitySilently()
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

  private fun isDeleted(id: ID): Boolean = deleted?.contains(id) ?: false

  private fun requireNotDeleted(id: ID) {
    if (isDeleted(id)) throw IllegalStateException("实体已被删除: ${entityClass.simpleName}($id)")
  }

  @Suppress("UNCHECKED_CAST")
  override fun flush(): Future<Unit> {
    return persistService.batchInsertOrUpdate(entitySequence().toList()) as Future<Unit>
  }

  override fun dump(): String = Json.stringify(entitySequence().toList())

  private fun entitySequence(): Sequence<E> {
    return cache.values.asSequence().map { it.getEntitySilently() }
      .filterNotNull() + pendingPersistCache.values.asSequence()
  }

  private class CacheObj<ID : Any, E : Entity<ID>>(private val entity: E?, @Volatile var accessTime: Long) {
    var lastPersistTime = 0L

    @Volatile
    private var expired = 0

    fun isExpired() = expired == 1

    fun setExpired() {
      if (!ExpiredUpdater.compareAndSet(this, 0, 1)) {
        throw IllegalStateException("Entity Expired")
      }
    }

    fun getEntity(): E? {
      accessTime = currentMillis()
      return entity
    }

    /**
     * leave `accessTime` untouched
     */
    fun getEntitySilently(): E? {
      return entity
    }

    fun getId(): ID? {
      return entity?.id()
    }

    fun hasEntity(): Boolean = entity != null

    fun isEmpty(): Boolean = entity == null

    companion object {
      private val ExpiredUpdater = AtomicIntegerFieldUpdater.newUpdater(CacheObj::class.java, "expired")

      private val EmptyCacheObj = CacheObj<Any, Entity<Any>>(null, 0)

      @Suppress("UNCHECKED_CAST")
      fun <ID : Any, E : Entity<ID>> empty(): CacheObj<ID, E> {
        return EmptyCacheObj as CacheObj<ID, E>
      }
    }
  }
}
