package play.entity.cache

import com.typesafe.config.Config
import play.Log
import play.ShutdownCoordinator
import play.entity.Entity
import play.entity.EntityInt
import play.entity.EntityLong
import play.util.control.getCause
import play.util.logging.getLogger
import play.util.reflect.Reflect
import play.util.unsafeCast
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KClass
import kotlin.time.seconds
import play.inject.PlayInjector
import play.util.classOf

class EntityCacheManager @Inject constructor(
  private val factory: EntityCacheFactory,
  @Named("entity") private val conf: Config,
  shutdownCoordinator: ShutdownCoordinator,
  injector: PlayInjector
) {
  private val logger = getLogger()

  private val postLoadProcessors =
    injector.getInstancesOfType(classOf<EntityInitializer<Entity<Any>>>())
      .asSequence().map { p ->
        Reflect.getTypeArg(p.javaClass, EntityInitializer::class.java, 0) to p
      }.toMap()

  private val caches = ConcurrentHashMap<Class<*>, EntityCache<*, *>>()

  private val dumpDir = File(conf.getString("cache-dump-dir"))

  init {
    Log.info { "Using ${factory.javaClass.simpleName}" }
    checkUnhandledCacheDump()
    shutdownCoordinator.addShutdownTask("缓存数据入库") {
      caches.values.forEach { cache ->
        val result = cache.flush().getResult(60.seconds)
        if (result.isFailure) {
          logger.error(result.getCause()) { "[${cache.entityClass.simpleName}]缓存数据入库失败，尝试保存到文件" }
          cacheDump(cache, dumpDir)
        }
      }
    }
  }

  private fun checkUnhandledCacheDump() {
    val dir = dumpDir
    if (dir.exists() && !dir.isDirectory) {
      throw IllegalStateException("${dir.absolutePath}不是文件夹")
    }
    if (dir.exists() && dir.isDirectory && !dir.list().isNullOrEmpty()) {
      throw UnhandledCacheDumpException(dir)
    }
  }

  fun cacheDump() {
    caches.values.forEach { cache ->
      cacheDump(cache, dumpDir)
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
      logger.info { "[$simpleName]缓存数据保存成功： $file" }
    } catch (e: Exception) {
      logger.error(e) { "[$simpleName]缓存数据保存失败" }
    }
  }

  fun <ID : Any, E : Entity<ID>> get(clazz: KClass<E>): EntityCache<ID, E> {
    return get(clazz.java)
  }

  @Suppress("UNCHECKED_CAST")
  fun <ID : Any, E : Entity<ID>> get(clazz: Class<E>): EntityCache<ID, E> {
    val cache = caches[clazz]
    if (cache != null) {
      return cache as EntityCache<ID, E>
    }
    val processor = postLoadProcessors[clazz] ?: DefaultEntityInitializer
    return caches.computeIfAbsent(clazz) {
      factory.create(it as Class<E>, processor.unsafeCast())
    }.unsafeCast()
  }

  @Suppress("UNCHECKED_CAST")
  fun <E : EntityLong> getEntityCacheLong(clazz: Class<E>): EntityCacheLong<E> {
    return get(clazz) as EntityCacheLong<E>
  }

  @Suppress("UNCHECKED_CAST")
  fun <E : EntityInt> getEntityCacheInt(clazz: Class<E>): EntityCacheInt<E> {
    return get(clazz) as EntityCacheInt<E>
  }
}
