// package play.db.cache
//
// import com.google.common.collect.Sets
// import play.Log
// import play.db.*
// import play.inject.Injector
// import play.util.concurrent.Future
// import play.util.control.getCause
// import play.util.getOrNull
// import play.util.json.Json
// import play.util.scheduling.Scheduler
// import play.util.time.currentMillis
// import play.util.toOptional
// import java.util.*
// import java.util.concurrent.ConcurrentHashMap
// import java.util.concurrent.ConcurrentMap
// import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
// import javax.annotation.Nullable
// import kotlin.NoSuchElementException
// import kotlin.time.minutes
// import kotlin.time.seconds
//
// internal class EntityCacheImpl2<ID : Any, E : Entity<ID>>(
//  entityClass: Class<E>,
//  private val persistService: PersistService,
//  private val queryService: QueryService,
//  injector: Injector,
//  scheduler: Scheduler,
//  executor: DbExecutor,
//  private val conf: DefaultEntityCacheFactory.Config,
//  private val entityProcessor: EntityProcessor<E>
// ) : AbstractEntityCache<ID, E>(entityClass, injector) {
//
//  private val cache: ConcurrentMap<ID, CacheObj<ID, E>> = ConcurrentHashMap(getInitialSizeOrDefault(conf.initialSize))
//  private val pendingPersistCache: ConcurrentMap<ID, E> = ConcurrentHashMap()
//
//  @Volatile
//  private var deleted: MutableSet<ID>? = null
//
//  init {
//    val cacheSpec = entityClass.getAnnotation(CacheSpec::class.java)
//    if (cacheSpec?.loadAllOnInit == true) {
//      Log.info { "loading all [${entityClass.simpleName}]" }
//      queryService.foreach(entityClass) {
//        entityProcessor.postLoad(it)
//        cache[it.id()] = CacheObj(it, currentMillis())
//      }.await(1.minutes)
//      Log.info { "loaded ${cache.size} [${entityClass.simpleName}]" }
//    }
//    val persistStrategy = cacheSpec?.persistStrategy ?: CacheSpec.PersistStrategy.Scheduled
//    if (persistStrategy == CacheSpec.PersistStrategy.Scheduled) {
//      scheduler.scheduleAtFixedRate(
//        conf.persistInterval,
//        conf.persistInterval.dividedBy(2),
//        executor,
//        createPersistTask()
//      )
//    } else {
//      Log.info { "[${entityClass.simpleName}] using [$persistStrategy] persist strategy." }
//    }
//
//    if (expireEvaluator !is NeverExpireEvaluator) {
//      scheduler.scheduleAtFixedRate(
//        conf.expireAfterAccess,
//        conf.expireAfterAccess.dividedBy(2),
//        executor,
//        createExpirationTask()
//      )
//    } else {
//      Log.info { "[${entityClass.simpleName}] will never expire." }
//    }
//  }
//
//  private fun createPersistTask(): () -> Unit {
//    return {
//      val now = currentMillis()
//      val persistTimeThreshold = now - conf.persistInterval.toMillis()
//      val entities = cache.values.asSequence()
//        .filter { it.hasEntity() && it.lastPersistTime < persistTimeThreshold }
//        .map {
//          it.lastPersistTime = now
//          it.getEntitySilently()!!
//        }
//        .toList()
//      if (entities.isNotEmpty()) {
//        persistService.batchInsertOrUpdate(entities)
//      }
//    }
//  }
//
//  private fun createExpirationTask(): () -> Unit {
//    return {
//      val accessTimeThreshold = currentMillis() - conf.expireAfterAccess.toMillis()
//      cache.values.asSequence()
//        .filter { it.hasEntity() && it.accessTime <= accessTimeThreshold && expireEvaluator.canExpire(it.getEntitySilently()!!) }
//        .forEach {
//          cache.computeIfPresent(it.getId()) { _, v ->
//            if (v.accessTime > accessTimeThreshold) {
//              v
//            } else {
//              v.setExpired()
//              if (v.hasEntity()) {
//                writeToDB(v.getEntitySilently()!!)
//              }
//              null
//            }
//          }
//        }
//    }
//  }
//
//  private fun writeToDB(e: E) {
//    val id: ID = e.id()
//    pendingPersistCache[id] = e
//    persistService.insertOrUpdate(e).onComplete { result ->
//      if (result.isSuccess) {
//        pendingPersistCache.remove(id)
//      } else {
//        // restore into cache
//        cache.compute(id) { k, v ->
//          val pending = pendingPersistCache.remove(k)
//          when {
//            pending == null -> v
//            v == null -> CacheObj(pending, currentMillis())
//            else -> { // v !=null && pending != null
//              val cacheEntity = v.getEntitySilently()
//              if (cacheEntity != null && cacheEntity !== pending) { // should be the same
//                logger.error {
//                  """
//                  缓存中对象与持久化队列中的对象不一致:
//                  cache: ${Json.stringify(cacheEntity)}
//                  queue: ${Json.stringify(pending)}
//                """.trimIndent()
//                }
//              }
//              v
//            }
//          }
//        }
//        logger.error(result.getCause()) { "持久化失败: ${entityClass.simpleName}($id)" }
//      }
//    }
//  }
//
//  override fun entityClass(): Class<E> {
//    return entityClass
//  }
//
//  override fun get(id: ID): Optional<E> {
//    return getOrNull(id).toOptional()
//  }
//
//  @Nullable
//  override fun getOrNull(id: ID): E? {
//    return computeIfAbsent(id, dbLoader)
//  }
//
//  override fun getOrThrow(id: ID): E {
//    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
//  }
//
//  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
//    requireNotDeleted(id)
//    return computeIfAbsent(id, true) {
//      var entity = dbLoader(it)
//      if (entity == null) {
//        entity = creation(it)
//        entityProcessor.postLoad(entity)
//        persistService.insert(entity).onFailure { e ->
//          logger.error(e) { "数据插入失败: ${entityClass.simpleName}${Json.stringify(entity)}" }
//        }
//      }
//      entity
//    }!!
//  }
//
//  @Suppress("UNCHECKED_CAST")
//  override fun getCached(id: ID): Optional<E> {
//    return computeIfAbsent(id, NOOP as (ID) -> E?).toOptional()
//  }
//
//  override fun asSequence(): Sequence<E> {
//    return cache.values.asSequence().map { it.getEntitySilently() }.filterNotNull()
//  }
//
//  override fun create(e: E): E {
//    requireNotDeleted(e.id())
//    val that = getOrCreate(e.id()) { e }
//    if (e !== that) {
//      throw EntityExistsException(e.javaClass, e.id())
//    }
//    return e
//  }
//
//  override fun remove(e: E) {
//    removeById(e.id())
//  }
//
//  override fun removeById(id: ID) {
//    cache.compute(id) { k, _ ->
//      if (delete(k)) {
//        persistService.deleteById(k, entityClass)
//      }
//      null
//    }
//  }
//
//  override fun save(e: E) {
//    requireNotDeleted(e.id())
//    val opt = getCached(e.id())
//    if (opt.isPresent && opt.get() !== e) {
//      throw IllegalStateException("${entityClass().simpleName}(${e.id()})保存失败，与缓存中的对象不一致")
//    } else {
//      persistService.update(e)
//    }
//  }
//
//  override fun size(): Int {
//    return cache.size
//  }
//
//  override fun isCached(id: ID): Boolean {
//    return cache.containsKey(id)
//  }
//
//  override fun isEmpty(): Boolean {
//    return cache.isEmpty()
//  }
//
//  private val dbLoader: (ID) -> E? = { id ->
//    val pendingPersist = pendingPersistCache.remove(id)
//    if (pendingPersist != null) {
//      pendingPersist
//    } else {
//      val f = queryService.findById(id, entityClass)
//      try {
//        val entity: E? = f.get(5.seconds).getOrNull()
//        if (entity != null) {
//          entityProcessor.postLoad(entity)
//        }
//        entity
//      } catch (e: Exception) {
//        logger.error(e) { "查询数据库失败: ${entityClass.simpleName}($id)" }
//        throw e
//      }
//    }
//  }
//
//  private fun computeIfAbsent(id: ID, loader: (ID) -> E?): E? {
//    return computeIfAbsent(id, false, loader)
//  }
//
//  private fun computeIfAbsent(id: ID, createIfAbsent: Boolean, loader: (ID) -> E?): E? {
//    var cacheObj = cache[id]
//    if (cacheObj != null) {
//      if (cacheObj.isEmpty() && !createIfAbsent) {
//        return null
//      }
//      val entity = cacheObj.getEntity()
//      if (entity != null && !cacheObj.isExpired()) {
//        return entity
//      }
//    }
//    if (loader === NOOP) return null
//
//    cacheObj = cache.compute(id) { k, obj ->
//      if (isDeleted(k)) {
//        null
//      } else if (obj == null || obj.isEmpty()) {
//        val v = loader(k)
//        if (v == null) CacheObj.empty() else CacheObj(v, currentMillis())
//      } else {
//        obj.accessTime = currentMillis()
//        obj
//      }
//    }
//    return cacheObj?.getEntitySilently()
//  }
//
//  private fun delete(id: ID): Boolean {
//    if (deleted == null) {
//      synchronized(this) {
//        if (deleted == null) {
//          deleted = Sets.newConcurrentHashSet()
//        }
//      }
//    }
//    return deleted!!.add(id)
//  }
//
//  private fun isDeleted(id: ID): Boolean = deleted?.contains(id) ?: false
//
//  private fun requireNotDeleted(id: ID) {
//    if (isDeleted(id)) throw IllegalStateException("实体已被删除: ${entityClass.simpleName}($id)")
//  }
//
//  @Suppress("UNCHECKED_CAST")
//  override fun flush(): Future<Unit> {
//    return persistService.batchInsertOrUpdate(entitySequence().toList()) as Future<Unit>
//  }
//
//  override fun dump(): String = Json.stringify(entitySequence().toList())
//
//  private fun entitySequence(): Sequence<E> {
//    return cache.values.asSequence().map { it.getEntitySilently() }
//      .filterNotNull() + pendingPersistCache.values.asSequence()
//  }
//
//  private class CacheObj<ID : Any, E : Entity<ID>>(private val entity: E?, @Volatile var accessTime: Long) {
//    var lastPersistTime = 0L
//
//    @Volatile
//    private var expired = 0
//
//    fun isExpired() = expired == 1
//
//    fun setExpired() {
//      if (!ExpiredUpdater.compareAndSet(this, 0, 1)) {
//        throw IllegalStateException("Entity Expired")
//      }
//    }
//
//    fun getEntity(): E? {
//      accessTime = currentMillis()
//      return entity
//    }
//
//    /**
//     * leave `accessTime` untouched
//     */
//    fun getEntitySilently(): E? {
//      return entity
//    }
//
//    fun getId(): ID? {
//      return entity?.id()
//    }
//
//    fun hasEntity(): Boolean = entity != null
//
//    fun isEmpty(): Boolean = entity == null
//
//    companion object {
//      private val ExpiredUpdater = AtomicIntegerFieldUpdater.newUpdater(CacheObj::class.java, "expired")
//
//      private val EmptyCacheObj = CacheObj<Any, Entity<Any>>(null, 0) // fixme create new
//
//      @Suppress("UNCHECKED_CAST")
//      fun <ID : Any, E : Entity<ID>> empty(): CacheObj<ID, E> {
//        return EmptyCacheObj as CacheObj<ID, E>
//      }
//    }
//  }
// }
