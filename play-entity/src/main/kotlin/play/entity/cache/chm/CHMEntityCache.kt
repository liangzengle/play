package play.entity.cache.chm

import mu.KLogging
import play.entity.Entity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.concurrent.Future
import play.util.control.getCause
import play.util.getOrNull
import play.util.json.Json
import play.util.time.Time.currentMillis
import play.util.toOptional
import play.util.unsafeCast
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

class CHMEntityCache<ID : Any, E : Entity<ID>>(
  override val entityClass: Class<E>,
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: AbstractEntityCacheFactory.Settings,
  private val initializerProvider: EntityInitializerProvider
) : EntityCache<ID, E>, UnsafeEntityCacheOps<ID>, EntityCacheInternalApi {
  companion object : KLogging()

  private var initialized = false

  private var _cache: ConcurrentMap<ID, CacheObj<ID, E>>? = null

  private lateinit var initializer: EntityInitializer<E>

  private lateinit var expireEvaluator: ExpireEvaluator

  private val persistingEntities: ConcurrentMap<ID, E> = ConcurrentHashMap()

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  private fun getCache(): ConcurrentMap<ID, CacheObj<ID, E>> {
    val cache = _cache
    if (cache != null) {
      return cache
    }
    ensureInitialized()
    return _cache!!
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
      expireEvaluator = if (cacheSpec != null && cacheSpec.neverExpire) NeverExpireEvaluator else {
        when (val expireEvaluator = cacheSpec?.expireEvaluator) {
          null -> DefaultExpireEvaluator
          DefaultExpireEvaluator::class -> DefaultExpireEvaluator
          NeverExpireEvaluator::class -> NeverExpireEvaluator
          else -> injector.getInstance(expireEvaluator.java)
        }
      }
      initializer = initializerProvider.get(entityClass)

      EntityCacheHelper.reportMissingInitialCacheSize(entityClass)
      val initialCacheSize = EntityCacheHelper.getInitialSizeOrDefault(entityClass, settings.initialSize)
      val cache = ConcurrentHashMap<ID, CacheObj<ID, E>>(initialCacheSize)
      val isLoadAllOnInit = cacheSpec?.loadAllOnInit ?: false
      if (isLoadAllOnInit) {
        logger.debug { "Loading all [${entityClass.simpleName}]" }
        entityCacheLoader.loadAll(entityClass, cache) { c, e ->
          initializer.initialize(e)
          c[e.id()] = CacheObj(e)
          c
        }.await(Duration.ofSeconds(5))
        logger.debug { "Loaded ${cache.size} [${entityClass.simpleName}] into cache." }
      }
      this._cache = cache

      val isNeverExpire = expireEvaluator == NeverExpireEvaluator
      if (!isNeverExpire) {
        val duration =
          if ((cacheSpec?.expireAfterAccess ?: 0) > 0) Duration.ofSeconds(cacheSpec.expireAfterAccess.toLong())
          else settings.expireAfterAccess
        val durationMillis = duration.toMillis()
        scheduler.scheduleWithFixedDelay(
          duration, duration.dividedBy(2), executor
        ) { scheduledExpire(durationMillis) }
      }

      if (!isImmutable) {
        scheduler.scheduleWithFixedDelay(
          settings.persistInterval, settings.persistInterval.dividedBy(2), executor, this::scheduledPersist
        )
      }
      initialized = true
    }
  }

  private fun scheduledPersist() {
    val now = currentMillis()
    val persistTimeThreshold = now - settings.persistInterval.toMillis()
    val entities = getCache().values.asSequence().filterIsInstance<NonEmpty<ID, E>>()
      .filter { it.lastAccessTime() < persistTimeThreshold }.map {
        it.lastPersistTime = now
        it.peekEntity()
      }.toList()
    if (entities.isNotEmpty()) {
      entityCacheWriter.batchInsertOrUpdate(entities)
    }
  }

  private fun scheduledExpire(expireAfterAccess: Long) {
    val cache = getCache()
    val accessTimeThreshold = currentMillis() - expireAfterAccess
    cache.values.asSequence().filter {
      it.lastAccessTime() <= accessTimeThreshold && (it.isEmpty() || expireEvaluator.canExpire(
        it.asNonEmpty().peekEntity()
      ))
    }.forEach {
      cache.computeIfPresent(it.id()) { _, v ->
        if (v.lastAccessTime() > accessTimeThreshold) {
          v
        } else {
          if (v is NonEmpty<ID, E>) {
            v.setExpired()
            persistOnExpired(v.peekEntity())
          }
          null
        }
      }
    }
  }

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
        getCache().compute(id) { k, v ->
          val pending = persistingEntities.remove(k)
          if (pending == null || pending.isDeleted()) v else v ?: CacheObj(pending)
        }
        logger.error(result.getCause()) { "持久化失败, 重新返回缓存: ${entityClass.simpleName}($id)" }
      }
    }
  }

  private fun load(id: ID): E? {
    val pendingPersist = persistingEntities.remove(id)
    if (pendingPersist != null) {
      return pendingPersist
    }
    val f = entityCacheLoader.loadById(id, entityClass)
    try {
      val entity: E? = f.blockingGet(settings.loadTimeout).getOrNull()
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

  private fun computeIfAbsent(id: ID, loader: ((ID) -> E?)?): E? {
    return computeIfAbsent(id, null, loader)
  }

  private fun computeIfAbsent(
    id: ID, loadOnEmpty: ((ID) -> E)?, loadOnAbsent: ((ID) -> E?)?
  ): E? {
    val cache = getCache()
    var cacheObj = cache[id]
    if (cacheObj != null) {
      if (cacheObj is NonEmpty<ID, E>) {
        val entity = cacheObj.accessEntity()
        if (!cacheObj.isExpired() || cache.putIfAbsent(id, CacheObj(entity)) == null) {
          return entity
        }
      } else if (loadOnEmpty == null) {
        return null
      }
    }
    if (loadOnEmpty == null && loadOnAbsent == null) return null
    cacheObj = cache.compute(id) { k, v ->
      if (v == null) {
        if (loadOnAbsent == null) null else loadOnAbsent(k)?.let { CacheObj(it) } ?: CacheObj(k)
      } else if (v.isEmpty() && loadOnEmpty != null) {
        CacheObj(loadOnEmpty(k))
      } else {
        if (v is NonEmpty) {
          v.accessEntity()
        }
        v
      }
    }
    return if (cacheObj is NonEmpty) cacheObj.accessEntity() else null
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
    return computeIfAbsent(id, { k -> createEntity(k, creation) }, { k -> load(k) ?: createEntity(k, creation) })
      ?: error("won't happen")
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

  override fun getCachedEntities(): Sequence<E> {
    return getCache().values.asSequence().filterIsInstance<NonEmpty<ID, E>>().map { it.peekEntity() }
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
    val loaded = entityCacheLoader.loadAll(missing, entityClass).blockingGet(Duration.ofSeconds(5))
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
    val that = getOrCreate(e.id()) { e }
    if (e !== that) {
      throw EntityExistsException(e.javaClass, e.id())
    }
    return e
  }

  override fun delete(e: E) {
    delete(e.id())
  }

  override fun delete(id: ID) {
    val cache = getCache()
    cache.compute(id) { k, v ->
      persistingEntities.remove(k)
      if (v is NonEmpty) {
        v.peekEntity().setDeleted()
        deleteFromDB(k)
      }
      CacheObj(k)
    }
  }

  override fun size(): Int {
    return getCache().size
  }

  override fun isCached(id: ID): Boolean {
    return getCache().containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return getCache().isEmpty()
  }

  override fun dump(): String {
    return Json.stringify(copyToMap().values)
  }

  @Suppress("UNCHECKED_CAST")
  override fun persist(): Future<Unit> {
    if (!initialized) {
      return Future.successful(Unit)
    }
    val entities = getCache().values.asSequence().filterIsInstance<NonEmpty<ID, E>>()
      .filter { !(isImmutable && it.lastPersistTime != 0L) }.map { it.peekEntity() }
      .plus(persistingEntities.values.asSequence()).toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun expireEvaluator(): ExpireEvaluator = expireEvaluator

  override fun initWithEmptyValue(id: ID) {
    val prev = getCache().putIfAbsent(id, CacheObj(id))
    if (prev?.isNotEmpty() == false) {
      logger.warn { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  private fun deleteFromDB(id: ID) {
    entityCacheWriter.deleteById(id, entityClass).onFailure {
      // TODO what to do if failed
      logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
    }
  }

  private fun copyToMap(): Map<ID, E> {
    val cache = getCache()
    val result = HashMap<ID, E>(cache.size + persistingEntities.size)
    for (entity in persistingEntities.values) {
      result[entity.id()] = entity
    }
    for (obj in cache.values) {
      if (obj is NonEmpty<ID, E>) {
        result[obj.id()] = obj.peekEntity()
      }
    }
    return result
  }

  private sealed class CacheObj<ID : Any, E : Entity<ID>> {
    abstract fun id(): ID

    abstract fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    abstract fun lastAccessTime(): Long

    fun asEmpty(): Empty<ID, E> = this.unsafeCast()

    fun asNonEmpty(): NonEmpty<ID, E> = this.unsafeCast()

    companion object {
      operator fun <ID : Any, E : Entity<ID>> invoke(entity: E): NonEmpty<ID, E> = NonEmpty(entity)
      operator fun <ID : Any, E : Entity<ID>> invoke(id: ID): Empty<ID, E> = Empty(id)
    }
  }

  private class Empty<ID : Any, E : Entity<ID>>(val id: ID, private val createTime: Long) : CacheObj<ID, E>() {
    constructor(id: ID) : this(id, currentMillis())

    override fun isEmpty(): Boolean = true

    override fun lastAccessTime(): Long = createTime

    override fun id(): ID = id

    override fun toString(): String {
      return "Empty($id)"
    }
  }

  private class NonEmpty<ID : Any, E : Entity<ID>>(
    private val entity: E, @field:Volatile private var lastAccessTime: Long
  ) : CacheObj<ID, E>() {

    constructor(entity: E) : this(entity, currentMillis())

    var lastPersistTime: Long = 0L

    @Volatile
    private var expired = 0

    fun isExpired() = expired == 1

    fun setExpired() {
      if (!ExpiredUpdater.compareAndSet(this, 0, 1)) {
        throw IllegalStateException("Entity Expired")
      }
    }

    override fun isEmpty(): Boolean = false

    override fun lastAccessTime(): Long = lastAccessTime

    fun peekEntity(): E = entity

    fun accessEntity(): E {
      lastAccessTime = currentMillis()
      return entity
    }

    override fun id(): ID = entity.id()

    override fun toString(): String {
      return entity.toString()
    }

    companion object {
      private val ExpiredUpdater: AtomicIntegerFieldUpdater<NonEmpty<*, *>> =
        AtomicIntegerFieldUpdater.newUpdater(NonEmpty::class.java, "expired")
    }
  }
}
