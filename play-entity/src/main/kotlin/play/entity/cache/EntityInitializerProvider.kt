package play.entity.cache

import play.entity.Entity
import play.inject.PlayInjector
import play.util.classOf
import play.util.collection.toImmutableMap
import play.util.reflect.Reflect
import play.util.unsafeCast
import play.util.unsafeLazy

/**
 *
 * @author LiangZengle
 */
class EntityInitializerProvider(private val injector: PlayInjector) {
  private val entityInitializers by unsafeLazy {
    injector.getInstancesOfType(classOf<EntityInitializer<Entity<Any>>>())
      .asSequence()
      .map { p -> Reflect.getTypeArg(p.javaClass, EntityInitializer::class.java, 0) to p }
      .toImmutableMap()
  }

  fun <T : Entity<*>> get(entityClass: Class<T>): EntityInitializer<T> {
    val initializer = entityInitializers[entityClass] ?: DefaultEntityInitializer
    return initializer.unsafeCast()
  }
}
