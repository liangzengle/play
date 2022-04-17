package play.entity.cache.chm

import mu.KLogging
import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.entity.IntIdEntity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.collection.ConcurrentIntObjectMap
import play.util.concurrent.Future
import play.util.control.getCause
import play.util.function.IntToObjFunction
import play.util.getOrNull
import play.util.json.Json
import play.util.time.Time.currentMillis
import play.util.toOptional
import play.util.unsafeCast
import java.time.Duration
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

class CHMEntityCacheInt<E : IntIdEntity>(
  override val entityClass: Class<E>,
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: EntityCacheFactory.Settings,
  private val initializerProvider: EntityInitializerProvider
) : EntityCache<Int, E>, UnsafeEntityCacheOps<Int>, EntityCacheInternalApi {
  companion object : KLogging()

  private var initialized = false

  private var _cache: ConcurrentIntObjectMap<CacheObj<E>>? = null

  private lateinit var initializer: EntityInitializer<E>

  private lateinit var expireEvaluator: ExpireEvaluator

  private val persistingEntities = ConcurrentIntObjectMap<E>()

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  private val isResident = EntityCacheHelper.isResident(entityClass)

  private fun getCache(): ConcurrentIntObjectMap<CacheObj<E>> {
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
      expireEvaluator = EntityCacheHelper.getExpireEvaluator(entityClass, injector)
      initializer = initializerProvider.get(entityClass)
      val cacheSpec = EntityCacheHelper.getCacheSpec(entityClass)

      EntityCacheHelper.reportMissingInitialCacheSize(entityClass)
      val initialCacheSize = EntityCacheHelper.getInitialSizeOrDefault(entityClass, settings.initialSize)
      val cache = ConcurrentIntObjectMap<CacheObj<E>>(initialCacheSize)
      val isLoadAllOnInit = cacheSpec.loadAllOnInit
      if (isLoadAllOnInit) {
        logger.debug { "Loading all [${entityClass.simpleName}]" }
        entityCacheLoader.loadAll(entityClass, cache) { c, e ->
          initializer.initialize(e)
          c[e.id] = NonEmpty(e)
          c
        }.await()
        logger.debug { "Loaded ${cache.size} [${entityClass.simpleName}] into cache." }
      }
      this._cache = cache

      val isNeverExpire = expireEvaluator == NeverExpireEvaluator
      if (!isNeverExpire) {
        val duration =
          if (cacheSpec.expireAfterAccess > 0) Duration.ofSeconds(cacheSpec.expireAfterAccess.toLong())
          else settings.expireAfterAccess
        val durationMillis = duration.toMillis()
        scheduler.scheduleWithFixedDelay(duration, duration.dividedBy(2), executor) { scheduledExpire(durationMillis) }
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
    val entities = getCache().values.asSequence().filterIsInstance<NonEmpty<E>>()
      .filter { it.lastAccessTime() < persistTimeThreshold }
      .map {
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
          if (v is NonEmpty<E>) {
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
    val id = e.id
    persistingEntities[id] = e
    entityCacheWriter.insertOrUpdate(e).onComplete { result ->
      if (result.isSuccess) {
        persistingEntities.remove(id)
      } else {
        // 写数据库失败，重新放回缓存
        getCache().compute(id) { k, v ->
          val pending = persistingEntities.remove(k)
          if (pending == null || pending.isDeleted()) v else v ?: NonEmpty(pending)
        }
        logger.error(result.getCause()) { "持久化失败, 重新返回缓存: ${entityClass.simpleName}($id)" }
      }
    }
  }

  private fun load(id: Int): E? {
    if (isResident) {
      return null
    }
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

  private fun computeIfAbsent(id: Int, loader: IntToObjFunction<E?>?): E? {
    return computeIfAbsent(id, null, loader)
  }

  private fun computeIfAbsent(id: Int, loadOnEmpty: IntToObjFunction<E>?, loadOnAbsent: IntToObjFunction<E?>?): E? {
    val cache = getCache()
    var cacheObj = cache[id]
    if (cacheObj != null) {
      if (cacheObj is NonEmpty<E>) {
        val entity = cacheObj.accessEntity()
        if (!cacheObj.isExpired() ||
          cache.putIfAbsent(id, NonEmpty(entity)) == null /* 这一刻刚好过期了，但是还没有从数据库重新加载，可以直接使用 */) {
          return entity
        }
      } else if (loadOnEmpty == null) {
        return null
      }
    }
    if (loadOnEmpty == null && loadOnAbsent == null) return null
    cacheObj = cache.compute(id) { k, v ->
      if (v == null) {
        if (loadOnAbsent == null) null else loadOnAbsent(k)?.let { NonEmpty(it) } ?: Empty(k)
      } else if (v.isEmpty() && loadOnEmpty != null) {
        NonEmpty(loadOnEmpty(k))
      } else {
        if (v is NonEmpty) v.accessEntity() // update access time
        v
      }
    }
    return if (cacheObj is NonEmpty) cacheObj.accessEntity() else null
  }

  override fun get(id: Int): Optional<E> {
    return getOrNull(id).toOptional()
  }

  override fun getOrNull(id: Int): E? {
    return computeIfAbsent(id, ::load)
  }

  override fun getOrThrow(id: Int): E {
    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
  }

  override fun getOrCreate(id: Int, creation: (Int) -> E): E {
    return computeIfAbsent(id, { k -> createEntity(k, creation) }, { k -> load(k) ?: createEntity(k, creation) })
      ?: error("won't happen")
  }

  private fun createEntity(id: Int, creation: IntToObjFunction<E>): E {
    val entity = creation(id)
    initializer.initialize(entity)
    entityCacheWriter.insert(entity)
    return entity
  }

  override fun getCached(id: Int): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun getCachedEntities(): Sequence<E> {
    return getCache().values.asSequence().filterIsInstance<NonEmpty<E>>().map { it.peekEntity() }
  }

  override fun getAll(ids: Iterable<Int>): List<E> {
    val result = arrayListOf<E>()
    val missing = arrayListOf<Int>()
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
      val e = computeIfAbsent(entity.id, null) { entity }
      if (e != null) {
        result.add(e)
      }
    }
    return result
  }

  override fun create(e: E): E {
    val that = getOrCreate(e.id) { e }
    if (e !== that) {
      throw EntityExistsException(e.javaClass, e.id)
    }
    return e
  }

  override fun delete(e: E) {
    delete(e.id)
  }

  override fun delete(id: Int) {
    val cache = getCache()
    cache.compute(id) { k, v ->
      persistingEntities.remove(k)
      if (v is NonEmpty) {
        v.peekEntity().setDeleted()
        deleteFromDB(k)
      }
      Empty(k)
    }
  }

  override fun size(): Int {
    return getCache().size
  }

  override fun isCached(id: Int): Boolean {
    return getCache().containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return getCache().isEmpty()
  }

  override fun dump(): String {
    return Json.stringify(copyToMap().values())
  }

  @Suppress("UNCHECKED_CAST")
  override fun persist(): Future<Unit> {
    if (!initialized) {
      return Future.successful(Unit)
    }
    val entities = getCache().values.asSequence().filterIsInstance<NonEmpty<E>>()
      .filter { !(isImmutable && it.lastPersistTime != 0L) }.map { it.peekEntity() }
      .plus(persistingEntities.values.asSequence()).toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun expireEvaluator(): ExpireEvaluator = expireEvaluator

  override fun initWithEmptyValue(id: Int) {
    val prev = getCache().putIfAbsent(id, Empty(id))
    if (prev?.isNotEmpty() == false) {
      logger.warn { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  private fun deleteFromDB(id: Int) {
    entityCacheWriter.deleteById(id, entityClass).onFailure {
      // TODO what to do if failed
      logger.error(it) { "${entityClass.simpleName}($id)删除失败" }
    }
  }

  private fun copyToMap(): IntObjectMap<E> {
    val cache = getCache()
    val result = IntObjectMaps.mutable.withInitialCapacity<E>(cache.size)
    for (entity in persistingEntities.values) {
      result.put(entity.id, entity)
    }
    for (obj in cache.values) {
      if (obj is NonEmpty<E>) {
        val entity = obj.peekEntity()
        result.put(entity.id, entity)
      }
    }
    return result
  }

  private sealed class CacheObj<E : IntIdEntity> {
    abstract fun id(): Int

    abstract fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    abstract fun lastAccessTime(): Long

    fun asEmpty(): Empty<E> = this.unsafeCast()

    fun asNonEmpty(): NonEmpty<E> = this.unsafeCast()
  }

  private class Empty<E : IntIdEntity>(val id: Int, private val createTime: Long) : CacheObj<E>() {
    constructor(id: Int) : this(id, currentMillis())

    override fun isEmpty(): Boolean = true

    override fun lastAccessTime(): Long = createTime

    override fun id(): Int = id

    override fun toString(): String {
      return "Empty($id)"
    }
  }

  private class NonEmpty<E : IntIdEntity>(
    private val entity: E, @field:Volatile private var lastAccessTime: Long
  ) : CacheObj<E>() {

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

    fun accessEntity(): E {
      lastAccessTime = currentMillis()
      return entity
    }

    override fun isEmpty(): Boolean = false

    override fun lastAccessTime(): Long = lastAccessTime

    fun peekEntity(): E = entity

    override fun id(): Int = entity.id

    override fun toString(): String {
      return entity.toString()
    }

    companion object {
      private val ExpiredUpdater: AtomicIntegerFieldUpdater<NonEmpty<*>> =
        AtomicIntegerFieldUpdater.newUpdater(NonEmpty::class.java, "expired")
    }
  }
}
