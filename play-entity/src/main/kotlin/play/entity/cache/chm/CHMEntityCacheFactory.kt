package play.entity.cache.chm

import com.google.inject.Injector
import com.typesafe.config.Config
import java.util.concurrent.Executor
import play.entity.Entity
import play.entity.EntityInt
import play.entity.EntityLong
import play.entity.cache.*
import play.scheduling.Scheduler
import play.util.isAssignableFrom
import play.util.unsafeCast
import javax.inject.Inject
import javax.inject.Named

class CHMEntityCacheFactory @Inject constructor(
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val injector: Injector,
  @Named("entity") entityConf: Config
) : AbstractEntityCacheFactory(entityConf.getConfig("cache")) {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializer: EntityInitializer<E>
  ): EntityCache<ID, E> {
    checkEntityClass(entityClass)
    return when {
      isAssignableFrom<EntityLong>(entityClass) -> {
        EntityCacheLongImpl(
          entityClass.unsafeCast<Class<EntityLong>>(),
          entityCacheWriter,
          entityCacheLoader,
          injector,
          scheduler,
          executor,
          initializer.unsafeCast(),
          settings
        ).unsafeCast()
      }
      isAssignableFrom<EntityInt>(entityClass) -> {
        EntityCacheIntImpl(
          entityClass.unsafeCast<Class<EntityInt>>(),
          entityCacheWriter,
          entityCacheLoader,
          injector,
          scheduler,
          executor,
          initializer.unsafeCast(),
          settings
        ).unsafeCast()
      }
      else -> {
        EntityCacheImpl(
          entityClass,
          entityCacheWriter,
          entityCacheLoader,
          injector,
          scheduler,
          executor,
          initializer,
          settings
        )
      }
    }
  }
}
