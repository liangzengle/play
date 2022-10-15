package play.entity.cache.chm

import mu.KLogging
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.impl.factory.primitive.LongLists
import org.eclipse.collectionx.toJava
import play.entity.LongIdEntity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.collection.ConcurrentLongObjectMap
import play.util.collection.LongIterable
import play.util.concurrent.Future
import play.util.concurrent.PlayFuture
import play.util.control.Retryable
import play.util.function.LongToObjFunction
import play.util.getOrNull
import play.util.json.Json
import play.util.time.Time.currentMillis
import play.util.toOptional
import play.util.unsafeCast
import java.time.Duration
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

class CHMEntityCacheLong<E : LongIdEntity>(
  override val entityClass: Class<E>,
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: EntityCacheFactory.Settings,
  private val initializerProvider: EntityInitializerProvider
) : EntityCacheLong<E>, UnsafeEntityCacheOps<Long>, EntityCacheInternalApi<E> {
  companion object : KLogging()

  private var initialized = false

  // intended non-volatile, I think it's fine in the scenario
  private var _cache: ConcurrentLongObjectMap<CacheObj<E>>? = null

  private lateinit var initializer: EntityInitializer<E>

  private lateinit var expireEvaluator: ExpireEvaluator

  private val isImmutable = entityClass.isAnnotationPresent(ImmutableEntity::class.java)

  private val isResident = EntityCacheHelper.isResident(entityClass)

  private fun getCache(): ConcurrentLongObjectMap<CacheObj<E>> {
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
      val cache = ConcurrentLongObjectMap<CacheObj<E>>(initialCacheSize)
      val isLoadAllOnInit = cacheSpec.loadAllOnInit
      if (isLoadAllOnInit) {
        logger.debug { "Loading all [${entityClass.simpleName}]" }
        entityCacheLoader.loadAll(entityClass).collect(
          { cache },
          { c, e ->
            initializer.initialize(e)
            c[e.id] = NonEmpty(e)
          }
        ).block()
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
    val entities = getCache().values.asSequence()
      .filterIsInstance<NonEmpty<E>>()
      .filter { it.lastAccessTime() > it.lastPersistTime }
      .map { it.peekEntity() }
      .toList()
    if (entities.isNotEmpty()) {
      batchPersist(entities)
    }
  }

  private fun batchPersist(entities: Collection<E>) {
    entityCacheWriter.batchInsertOrUpdate(entities)
      .onSuccess { entities.forEach(::onPersistSucceeded) }
      .onFailure { e ->
        logger.error(e) { "[${entityClass.simpleName}] batch upsert failed, fallback to one by one" }
        entities.forEach(::singlePersist)
      }
  }

  private fun singlePersist(entity: E) {
    entityCacheWriter.insertOrUpdate(entity)
      .onSuccess { onPersistSucceeded(entity) }
      .onFailure { e ->
        logger.error(e) {
          "${entityClass.simpleName}(${entity.id}) upsert failed: ${Json.stringify(entity)}"
        }
      }
  }

  private fun onPersistSucceeded(entity: E) {
    val cache = getCache()
    val obj = cache[entity.id]
    if (obj is NonEmpty<E>) {
      obj.lastPersistTime = currentMillis()
    }
  }

  private fun scheduledExpire(expireAfterAccess: Long) {
    val cache = getCache()
    val accessTimeThreshold = currentMillis() - expireAfterAccess
    val expireKeys = LongLists.mutable.empty()
    for (entry in cache) {
      if (entry.value.lastAccessTime() <= accessTimeThreshold) {
        expireKeys.add(entry.key)
      }
    }
    for (i in 0 ..< expireKeys.size()) {
      val id = expireKeys[i]
      cache.computeIfPresent(id) { _, v ->
        if (v.lastAccessTime() > accessTimeThreshold) v
        else if (v is NonEmpty<E>) {
          if (v.lastPersistTime > v.lastAccessTime() && expireEvaluator.canExpire(v.peekEntity())) {
            v.setExpired()
            null
          } else v
        } else null
      }
    }
  }

  private fun load(id: Long): E? {
    if (isResident) {
      return null
    }
    val f = entityCacheLoader.loadById(entityClass, id)
    try {
      val entity: E = f.blockOptional(settings.loadTimeout).getOrNull() ?: return null
      if (entity.isDeleted()) {
        entityCacheWriter.deleteById(id, entityClass)
        return null
      }
      initializer.initialize(entity)
      return entity
    } catch (e: Exception) {
      logger.error(e) { "Failed to load entity: ${entityClass.simpleName}($id)" }
      throw e
    }
  }

  private fun computeIfAbsent(id: Long, loader: LongToObjFunction<E?>?): E? {
    return computeIfAbsent(id, null, loader)
  }

  private fun computeIfAbsent(id: Long, loadOnEmpty: LongToObjFunction<E>?, loadOnAbsent: LongToObjFunction<E?>?): E? {
    val cache = getCache()
    var cacheObj = cache[id]
    if (cacheObj != null) {
      if (cacheObj is NonEmpty<E>) {
        val entity = cacheObj.accessEntity()
        if (!cacheObj.isExpired() ||
          cache.putIfAbsent(
            id,
            NonEmpty(entity)
          ) == null /* 这一刻刚好过期了，但是还没有从数据库重新加载，可以直接使用 */) {
          return entity
        }
      } else if (loadOnEmpty == null) {
        return null
      }
    }
    if (loadOnEmpty == null && loadOnAbsent == null) return null
    cacheObj = cache.compute(id) { k, v ->
      if (v == null) {
        if (loadOnAbsent == null) null else loadOnAbsent(k)?.let { NonEmpty(it) } ?: Empty()
      } else if (v.isEmpty() && loadOnEmpty != null) {
        NonEmpty(loadOnEmpty(k))
      } else {
        if (v is NonEmpty) v.accessEntity() // update access time
        v
      }
    }
    return if (cacheObj is NonEmpty) cacheObj.accessEntity() else null
  }

  override fun get(id: Long): Optional<E> {
    return getOrNull(id).toOptional()
  }

  override fun getOrNull(id: Long): E? {
    return computeIfAbsent(id, ::load)
  }

  override fun getOrThrow(id: Long): E {
    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
  }

  override fun getOrCreate(id: Long, creation: (Long) -> E): E {
    return computeIfAbsent(id, { k -> createEntity(k, creation) }, { k -> load(k) ?: createEntity(k, creation) })
      ?: error("won't happen")
  }

  private fun createEntity(id: Long, creation: LongToObjFunction<E>): E {
    val entity = creation(id)
    initializer.initialize(entity)
    entityCacheWriter.insert(entity)
    return entity
  }

  override fun getCached(id: Long): Optional<E> {
    return computeIfAbsent(id, null).toOptional()
  }

  override fun getAllCached(): Sequence<E> {
    return getCache().values.asSequence().filterIsInstance<NonEmpty<E>>().map { it.peekEntity() }
  }

  override fun getAll(ids: Iterable<Long>): MutableList<E> {
    return getAll(LongIterable.fromJava(ids))
  }

  override fun getAll(ids: LongIterable): MutableList<E> {
    val result = Lists.mutable.empty<E>()
    val missing = LongLists.mutable.empty()
    for (id in ids) {
      val entity = getOrNull(id)
      if (entity != null) {
        result.add(entity)
      } else {
        missing.add(id)
      }
    }
    if (missing.isEmpty) {
      return result
    }
    val loaded = entityCacheLoader.loadAll(entityClass, missing.toJava()).collectList()
      .blockOptional(Duration.ofSeconds(10)).orElse(Collections.emptyList())
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

  override fun delete(id: Long) {
    getCache().compute(id) { _, _ ->
      entityCacheWriter.deleteById(id, entityClass).onFailure {
        logger.error(it) { "Delete entity failed: ${entityClass.simpleName}($id)" }
        retryDelete(id)
      }
      Empty()
    }
  }

  private fun retryDelete(id: Long) {
    // 防止Empty对象缓存过期:
    // 1. 重置间隔要小于过期时间
    // 2. 失败后需要刷新缓存
    Retryable.foreverAsync(
      "delete ${entityClass.simpleName}($id)",
      settings.expireAfterAccess.dividedBy(2).toMillis(),
      scheduler,
      executor
    ) {
      entityCacheWriter.deleteById(id, entityClass).unsafeCast<PlayFuture<Any?>>()
        .recoverWith { ex ->
          val cache = getCache()
          // 删除的实体被重新创建？
          if (cache[id] is NonEmpty<E>) {
            logger.warn { "Deleted entity has bean created again: ${entityClass.simpleName}($id)" }
            Future.successful(Unit)
          } else {
            // 刷新缓存，避免从数据加载一个被删除了的数据
            cache.compute(id) { _, v ->
              if (v is Empty<E>) Empty() else v
            }
            Future.failed(ex)
          }
        }
    }
  }

  override fun size(): Int {
    return getCache().size
  }

  override fun isCached(id: Long): Boolean {
    return getCache().containsKey(id)
  }

  override fun isEmpty(): Boolean {
    return getCache().isEmpty()
  }

  override fun dump(): String {
    return Json.prettyWriter().writeValueAsString(getAllCached().toList())
  }

  @Suppress("UNCHECKED_CAST")
  override fun persist(): Future<Unit> {
    if (!initialized) {
      return Future.successful(Unit)
    }
    val entities = getCache().values
      .asSequence()
      .filterIsInstance<NonEmpty<E>>()
      .filterNot { isImmutable && it.lastPersistTime != 0L }
      .map { it.peekEntity() }
      .toList()
    return entityCacheWriter.batchInsertOrUpdate(entities) as Future<Unit>
  }

  override fun expireEvaluator(): ExpireEvaluator = expireEvaluator

  override fun initWithEmptyValue(id: Long) {
    val prev = getCache().putIfAbsent(id, Empty())
    if (prev != null) {
      logger.debug { "初始化为空值失败, Entity已经存在: $prev" }
    }
  }

  private sealed class CacheObj<E : LongIdEntity> {
    abstract fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    abstract fun lastAccessTime(): Long

    fun asEmpty(): Empty<E> = this.unsafeCast()

    fun asNonEmpty(): NonEmpty<E> = this.unsafeCast()
  }

  private class Empty<E : LongIdEntity> : CacheObj<E>() {
    private val createTime: Long = currentMillis()

    override fun isEmpty(): Boolean = true

    override fun lastAccessTime(): Long = createTime

    override fun toString(): String {
      return "Empty"
    }
  }

  private class NonEmpty<E : LongIdEntity>(
    private val entity: E,
    _lastAccessTime: Long,
    _lastPersistTime: Long
  ) : CacheObj<E>() {

    constructor(entity: E) : this(entity, currentMillis(), 0)

    @Volatile
    var lastAccessTime: Long = _lastAccessTime
      private set

    @Volatile
    var lastPersistTime: Long = _lastPersistTime

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

    override fun toString(): String {
      return entity.toString()
    }

    companion object {
      private val ExpiredUpdater: AtomicIntegerFieldUpdater<NonEmpty<*>> =
        AtomicIntegerFieldUpdater.newUpdater(NonEmpty::class.java, "expired")
    }
  }
}
