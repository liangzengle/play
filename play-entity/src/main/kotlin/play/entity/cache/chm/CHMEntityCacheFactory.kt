package play.entity.cache.chm

import com.typesafe.config.Config
import play.entity.Entity
import play.entity.IntIdEntity
import play.entity.LongIdEntity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.isAssignableFrom
import play.util.unsafeCast
import java.util.concurrent.Executor

class CHMEntityCacheFactory(
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val injector: PlayInjector,
  cacheConf: Config
) : AbstractEntityCacheFactory(cacheConf) {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E> {
    return when {
      isAssignableFrom<LongIdEntity>(entityClass) -> {
        EntityCacheLongImpl(
          entityClass.unsafeCast<Class<LongIdEntity>>(),
          entityCacheWriter,
          entityCacheLoader,
          injector,
          scheduler,
          executor,
          initializerProvider,
          settings
        ).unsafeCast()
      }
      isAssignableFrom<IntIdEntity>(entityClass) -> {
        EntityCacheIntImpl(
          entityClass.unsafeCast<Class<IntIdEntity>>(),
          entityCacheWriter,
          entityCacheLoader,
          injector,
          scheduler,
          executor,
          initializerProvider,
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
          initializerProvider,
          settings
        )
      }
    }
  }
}
