package play.db.cache

import play.ApplicationLifecycle
import play.Configuration
import play.db.DefaultEntityProcessor
import play.db.Entity
import play.db.EntityProcessor
import play.getLogger
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableMap
import play.util.reflect.Reflect
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class EntityCacheManager @Inject constructor(
  private val factory: EntityCacheFactory,
  private val injector: Injector,
  @Named("db") private val conf: Configuration,
  lifecycle: ApplicationLifecycle
) : PostConstruct {

  private val logger = getLogger()

  private val caches = ConcurrentHashMap<Class<*>, EntityCache<*, *>>()

  private val dumpDir = File(conf.getString("cache-dump-dir"))

  private lateinit var entityProcessors: Map<Class<*>, EntityProcessor<*>>

  init {
    checkUnhandledCacheDump()
    lifecycle.addShutdownHook("缓存数据入库") {
      caches.values.forEach { cache ->
        val f = cache.flush()
        f.await(60, TimeUnit.SECONDS)
        if (f.isFailure) {
          logger.error(f.cause.get()) { "[${cache.entityClass().simpleName}]缓存数据入库失败，尝试保存到文件" }
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
    try {
      if (!outputDir.exists()) {
        outputDir.mkdirs()
      }
      val content = cache.dump()
      val file = outputDir.resolve("${cache.entityClass().simpleName}.json")
      file.writeText(content)
      logger.info { "[${cache.entityClass().simpleName}]缓存数据保存成功： $file" }
    } catch (e: Exception) {
      logger.error(e) { "[${cache.entityClass().simpleName}]缓存数据保存失败" }
    }
  }

  override fun postConstruct() {
    entityProcessors = injector.getInstancesOfType(EntityProcessor::class.java).asSequence()
      .map {
        Reflect.getRawClass<EntityProcessor<*>>(
          Reflect.getTypeArg(
            it.javaClass,
            EntityProcessor::class.java,
            0
          )
        ) to it
      }
      .toImmutableMap()
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
    return caches.computeIfAbsent(clazz) {
      val entityClass = it as Class<E>
      val entityProcessor = (entityProcessors[entityClass] ?: DefaultEntityProcessor) as EntityProcessor<E>
      factory.create(entityClass, entityProcessor)
    } as EntityCache<ID, E>
  }
}
