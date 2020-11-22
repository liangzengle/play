package play.db.cache

import play.ApplicationLifecycle
import play.Configuration
import play.Log
import play.db.*
import play.getLogger
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableMap
import play.util.control.getCause
import play.util.reflect.Reflect
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.time.seconds

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
    Log.info { "Using ${factory.javaClass.simpleName}" }
    checkUnhandledCacheDump()
    lifecycle.addShutdownHook("缓存数据入库") {
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

  @Suppress("UNCHECKED_CAST")
  fun <E : EntityLong> getEntityCacheLong(clazz: Class<E>): EntityCacheLong<E> {
    return get(clazz) as EntityCacheLong<E>
  }

  @Suppress("UNCHECKED_CAST")
  fun <E : EntityInt> getEntityCacheInt(clazz: Class<E>): EntityCacheInt<E> {
    return get(clazz) as EntityCacheInt<E>
  }
}
