package play.db.cache

import play.ApplicationLifecycle
import play.db.DefaultEntityProcessor
import play.db.Entity
import play.db.EntityProcessor
import play.inject.Injector
import play.inject.guice.PostConstruct
import play.util.collection.toImmutableMap
import play.util.reflect.Reflect
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class EntityCacheManager @Inject constructor(
  private val factory: EntityCacheFactory,
  private val injector: Injector,
  lifecycle: ApplicationLifecycle
) : PostConstruct {

  private val caches = ConcurrentHashMap<Class<*>, EntityCache<*, *>>()

  private lateinit var entityProcessors: Map<Class<*>, EntityProcessor<*>>

  init {
    val dir = File("db-cache-dump")
    if (dir.exists()) {
      throw UnhandledCacheDumpException(dir)
    }

    lifecycle.addShutdownHook("缓存数据入库") {
      caches.values.forEach { cache ->
        val f = cache.flush()
        f.await(60, TimeUnit.SECONDS)
        if (f.isFailure) {
          // TODO log
          var success = false
          while (!success) {
            try {
              val content = cache.dump()
              File("db-cache-dump/${cache.entityClass().simpleName}.json").writeText(content)
              success = true
              // TODO log
            } catch (e: Exception) {
              // TODO log
              Thread.sleep(10000)
            }
          }
        }
      }
    }
  }

  override fun postConstruct() {
    entityProcessors = injector.instancesOf(EntityProcessor::class.java).asSequence()
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
