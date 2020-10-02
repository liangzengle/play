package play.db.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.CacheWriter
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.google.common.collect.Sets
import io.vavr.control.Option
import io.vavr.kotlin.option
import play.Configuration
import play.db.Entity
import play.db.EntityProcessor
import play.db.PersistService
import play.db.QueryService
import play.inject.Injector
import play.util.concurrent.CommonPool
import play.util.concurrent.awaitSuccessOrThrow
import play.util.json.Json
import play.util.primitive.toIntSaturated
import play.util.unsafeCast
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.minutes

/**
 * Factory for creating CaffeineEntityCache
 * @author LiangZengle
 */
@Singleton
class CaffeineEntityCacheFactory @Inject constructor(
  private val persistService: PersistService,
  private val queryService: QueryService,
  private val injector: Injector,
  @Named("cache") conf: Configuration
) : EntityCacheFactory {

  private val config: Config

  init {
    val initialCacheSize = conf.getInt("initial-size")
    val expireAfterAccess = conf.getDuration("expire-after-access")
    config = Config(initialCacheSize, expireAfterAccess)
  }

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    entityProcessor: EntityProcessor<E>
  ): EntityCache<ID, E> {
    return CaffeineEntityCache(
      entityClass,
      persistService,
      queryService,
      injector,
      entityProcessor,
      config
    )
  }

  internal class Config(@JvmField val initialSize: Int, @JvmField val expireAfterAccess: Duration)
}

internal open class CaffeineEntityCache<ID : Any, E : Entity<ID>>(
  entityClass: Class<E>,
  private val persistService: PersistService,
  private val queryService: QueryService,
  injector: Injector,
  private val entityProcessor: EntityProcessor<E>,
  conf: CaffeineEntityCacheFactory.Config
) : AbstractEntityCache<ID, E>(entityClass, injector) {

  private val cache: Cache<ID, CacheObj<ID, E>>
  private val pendingPersistCache: ConcurrentMap<ID, E> = ConcurrentHashMap()

  @Volatile
  protected var deleted: MutableSet<ID>? = null

  private val evictShelter: ConcurrentMap<ID, E>? =
    if (expireEvaluator !is NeverExpireEvaluator) ConcurrentHashMap() else null

  init {
    val builder: Caffeine<ID, CacheObj<ID, E>> = Caffeine.newBuilder().unsafeCast()
    builder.initialCapacity(getInitialSizeOrDefault(conf.initialSize))
    if (expireEvaluator !is NeverExpireEvaluator) {
      builder.expireAfterAccess(conf.expireAfterAccess)
    }
    builder.writer(Writer())
    val cache: Cache<ID, CacheObj<ID, E>> = builder.build()

    if (isLoadAllOnInit()) {
      queryService.foreach(entityClass) {
        entityProcessor.postLoad(it)
        cache.put(it.id(), CacheObj(it))
      }.awaitSuccessOrThrow(1.minutes)
    }
    this.cache = cache
  }


  private fun load(id: ID): E? {
    val pendingPersist = pendingPersistCache.remove(id)
    if (pendingPersist != null) {
      return pendingPersist
    }
    val entityInShelter = evictShelter?.remove(id)
    if (entityInShelter != null) {
      return entityInShelter
    }
    val f = queryService.findById(id, entityClass)
    f.await(5, TimeUnit.SECONDS)
    val queryResult = f.value.get()
    if (queryResult.isFailure) {
      logger.error(queryResult.cause) { "查询数据库失败: ${entityClass.simpleName}($id)" }
      throw queryResult.cause
    } else {
      val entity: E? = queryResult.get().orNull
      if (entity != null) {
        entityProcessor.postLoad(entity)
      }
      return entity
    }
  }

  override fun entityClass(): Class<E> = entityClass

  override fun get(id: ID): Option<E> {
    return getOrNull(id).option()
  }

  override fun getOrNull(id: ID): E? {
    return computeIfAbsent(id, ::load)
  }

  override fun getOrThrow(id: ID): E {
    return getOrNull(id) ?: throw NoSuchElementException("${entityClass.simpleName}($id)")
  }

  override fun getOrCreate(id: ID, creation: (ID) -> E): E {
    return computeIfAbsent(id, true) { k ->
      var entity = load(k)
      if (entity == null) {
        entity = creation(k)
        entityProcessor.postLoad(entity)
        persistService.insert(entity)
      }
      entity
    } ?: error("won't happen")
  }


  override fun getCached(id: ID): Option<E> {
    return computeIfAbsent(id, NOOP.unsafeCast()).option()
  }

  override fun asSequence(): Sequence<E> {
    return cache.asMap().values.asSequence().map { it.entity!! }.filterNotNull()
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
    if (opt.isDefined && opt.get() !== e) {
      throw IllegalStateException("${entityClass().simpleName}(${e.id()})保存失败，与缓存中的对象不一致")
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

  private fun entitySequence(): Sequence<E> {
    return asSequence() + pendingPersistCache.values.asSequence()
  }

  private fun computeIfAbsent(id: ID, loader: (ID) -> E?): E? {
    return computeIfAbsent(id, false, loader)
  }

  private fun computeIfAbsent(id: ID, createIfAbsent: Boolean, loader: (ID) -> E?): E? {
    var cacheObj = cache.getIfPresent(id)
    if (cacheObj != null) {
      if (cacheObj.entity != null) {
        return cacheObj.entity
      } else if (!createIfAbsent) {
        return null
      }
    }
    if (loader === NOOP) return null

    cacheObj = cache.get(id) { k ->
      if (isDeleted(k)) {
        null
      } else {
        val v = loader(k)
        if (v == null) CacheObj.empty() else CacheObj(v)
      }
    }
    return cacheObj?.entity
  }

  private fun isDeleted(id: ID) = deleted?.contains(id) ?: false

  private class CacheObj<ID : Any, E : Entity<ID>>(val entity: E?) {
    companion object {
      private val Empty = CacheObj<Any, Entity<Any>>(null)
      fun <ID : Any, E : Entity<ID>> empty() = Empty.unsafeCast<CacheObj<ID, E>>()
    }
  }

  private inner class Writer : CacheWriter<ID, E> {
    override fun write(key: ID, value: E) {
    }

    override fun delete(id: ID, e: E?, cause: RemovalCause) {
      if (!cause.wasEvicted()) {
        if (delete(id)) {
          persistService.deleteById(id, entityClass)
        }
      } else if (e != null) {
        if (evictShelter != null && !expireEvaluator.canExpire(e)) {
          evictShelter[id] = e
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
    pendingPersistCache[id] = e
    persistService.update(e).onComplete { result ->
      if (result.isSuccess) {
        pendingPersistCache.remove(id)
      } else {
        cache.asMap().compute(id) { k, v ->
          val pending = pendingPersistCache.remove(k)
          when {
            pending == null -> v
            v == null -> CacheObj(pending)
            else -> error("should not happen")
          }
        }
        logger.error(result.cause) { "持久化失败: ${entityClass.simpleName}($id)" }
      }
    }
  }
}
