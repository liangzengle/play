package play.entity.cache

import mu.KLogging
import play.entity.Entity
import play.entity.IntIdEntity
import play.entity.LongIdEntity
import play.inject.PlayInjector
import play.util.ClassUtil
import play.util.control.getCause
import play.util.unsafeCast
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes

abstract class EntityCacheManager {
  abstract fun getAllCaches(): Iterable<EntityCache<*, *>>

  fun <ID, E : Entity<ID>> get(clazz: KClass<E>): EntityCache<ID, E> {
    return get(clazz.java)
  }

  abstract fun <ID, E : Entity<ID>> get(clazz: Class<E>): EntityCache<ID, E>

  fun <E : IntIdEntity> getEntityCacheInt(clazz: Class<E>): EntityCacheInt<E> {
    val cache = get(clazz)
    return if (cache is EntityCacheInt<E>) cache else EntityCacheIntWrapper(cache)
  }

  fun <E : LongIdEntity> getEntityCacheLong(clazz: Class<E>): EntityCacheLong<E> {
    val cache = get(clazz)
    return if (cache is EntityCacheLong<E>) cache else EntityCacheLongWrapper(cache)
  }
}

class EntityCacheManagerImpl constructor(
  private val factory: EntityCacheFactory,
  injector: PlayInjector,
  private val persistFailOver: EntityCachePersistFailOver
) : EntityCacheManager(), AutoCloseable {
  companion object : KLogging() {
    private val CLOSED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(EntityCacheManagerImpl::class.java, "closed")
  }

  private val initializerProvider = EntityInitializerProvider(injector)

  private val caches = ConcurrentHashMap<Class<*>, EntityCache<*, *>>()

  @Volatile
  private var closed = 0

  init {
    logger.info { "EntityCacheFactory: ${factory.javaClass.simpleName}" }
    logger.info { "EntityCachePersistFailOver: ${persistFailOver.javaClass.simpleName}" }
  }

  override fun close() {
    if (!CLOSED_UPDATER.compareAndSet(this, 0, 1)) {
      return
    }
    for (cache in caches.values) {
      val result = cache.persist().blockingGetResult(1.minutes)
      if (result.isFailure) {
        logger.error(result.getCause()) { "[${cache.entityClass.simpleName}]缓存数据入库失败，尝试使用[${persistFailOver.javaClass.simpleName}]处理" }
        try {
          persistFailOver.onPersistFailed(cache)
        } catch (e: Exception) {
          logger.error(e) { "Exception occurred when performing persist failover." }
        }
      }
    }
    logger.info { "EntityCache persisted on close." }
  }

  override fun getAllCaches(): Iterable<EntityCache<*, *>> {
    return Collections.unmodifiableCollection(caches.values)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <ID, E : Entity<ID>> get(clazz: Class<E>): EntityCache<ID, E> {
    val cache = caches[clazz]
    if (cache != null) {
      return cache as EntityCache<ID, E>
    }
    if (!ClassUtil.isInstantiatable(clazz)) {
      throw IllegalArgumentException("$clazz is not instantiatable.")
    }
    return caches.computeIfAbsent(clazz) { factory.create(it as Class<E>, initializerProvider) }.unsafeCast()
  }
}
