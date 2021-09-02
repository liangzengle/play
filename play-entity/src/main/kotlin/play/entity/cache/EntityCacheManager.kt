package play.entity.cache

import mu.KLogging
import play.ShutdownCoordinator
import play.entity.Entity
import play.entity.IntIdEntity
import play.entity.LongIdEntity
import play.inject.PlayInjector
import play.util.ClassUtil
import play.util.control.getCause
import play.util.unsafeCast
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration

abstract class EntityCacheManager {
  abstract fun getAllCaches(): Iterable<EntityCache<*, *>>

  fun <ID : Any, E : Entity<ID>> get(clazz: KClass<E>): EntityCache<ID, E> {
    return get(clazz.java)
  }

  abstract fun <ID : Any, E : Entity<ID>> get(clazz: Class<E>): EntityCache<ID, E>

  @Suppress("UNCHECKED_CAST")
  fun <E : LongIdEntity> getLongIdEntityCache(clazz: Class<E>): EntityCacheLong<E> {
    return get(clazz) as EntityCacheLong<E>
  }

  @Suppress("UNCHECKED_CAST")
  fun <E : IntIdEntity> getIntIdEntityCache(clazz: Class<E>): EntityCacheInt<E> {
    return get(clazz) as EntityCacheInt<E>
  }
}

class EntityCacheManagerImpl constructor(
  private val factory: EntityCacheFactory,
  shutdownCoordinator: ShutdownCoordinator,
  injector: PlayInjector,
  persistFailOver: EntityCachePersistFailOver
) : EntityCacheManager() {
  companion object : KLogging()

  private val initializerProvider = EntityInitializerProvider(injector)

  private val caches = ConcurrentHashMap<Class<*>, EntityCache<*, *>>()

  init {
    logger.info { "Using ${factory.javaClass.simpleName}" }
    logger.info { "Using ${persistFailOver.javaClass.simpleName}" }
    shutdownCoordinator.addShutdownTask("缓存数据入库") {
      caches.values.forEach { cache ->
        val result = cache.persist().getResult(Duration.seconds(60))
        if (result.isFailure) {
          logger.error(result.getCause()) { "[${cache.entityClass.simpleName}]缓存数据入库失败，尝试保存到文件" }
          persistFailOver.onPersistFailed(cache)
        }
      }
    }
  }

  override fun getAllCaches(): Iterable<EntityCache<*, *>> {
    return Collections.unmodifiableCollection(caches.values)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <ID : Any, E : Entity<ID>> get(clazz: Class<E>): EntityCache<ID, E> {
    val cache = caches[clazz]
    if (cache != null) {
      return cache as EntityCache<ID, E>
    }
    if (!ClassUtil.isTopLevelConcreteClass(clazz)) {
      throw IllegalArgumentException("$clazz should be a top level concrete class.")
    }
    return caches.computeIfAbsent(clazz) { factory.create(it as Class<E>, initializerProvider) }.unsafeCast()
  }
}

interface EntityCachePersistFailOver {
  fun onPersistFailed(entityCache: EntityCache<*, *>)
}

object NOOPEntityCachePersistFailOver : EntityCachePersistFailOver {
  override fun onPersistFailed(entityCache: EntityCache<*, *>) {
  }
}

class DefaultEntityCachePersistFailOver(dumpPath: String) : EntityCachePersistFailOver {

  companion object : KLogging()

  private val dumpDir = File(dumpPath)

  init {
    checkUnhandledCacheDump()
  }

  override fun onPersistFailed(entityCache: EntityCache<*, *>) {
    cacheDump(entityCache, dumpDir)
  }

  private fun checkUnhandledCacheDump() {
    val dir = dumpDir
    if (dir.exists() && !dir.isDirectory) {
      throw IllegalStateException("${dir.absolutePath}不是文件夹")
    }
    if (dir.exists() && dir.isDirectory && !dir.list().isNullOrEmpty()) {
      throw UnhandledEntityCacheDumpException(dir)
    }
  }

  private fun cacheDump(cache: EntityCache<*, *>, outputDir: File) {
    val simpleName = cache.entityClass.simpleName
    try {
      if (!outputDir.exists()) {
        outputDir.mkdirs()
      }
      val content = cache.dump()
      val file = outputDir.resolve("$simpleName.json")
      file.writeText(content)
      logger.info { "[$simpleName]缓存数据保存文件成功: $file" }
    } catch (e: Exception) {
      logger.error(e) { "[$simpleName]缓存数据保存文件失败" }
    }
  }
}
