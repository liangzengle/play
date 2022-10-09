package play.entity.cache.chm

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
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: EntityCacheFactory.Settings
) : EntityCacheFactory {

  override fun <ID, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E> {
    return if (isAssignableFrom<LongIdEntity>(entityClass)) {
      CHMEntityCacheLong(
        entityClass.unsafeCast<Class<LongIdEntity>>(),
        entityCacheWriter,
        entityCacheLoader,
        injector,
        scheduler,
        executor,
        settings,
        initializerProvider
      ).unsafeCast()
    } else if (isAssignableFrom<IntIdEntity>(entityClass)) {
      CHMEntityCacheInt(
        entityClass.unsafeCast<Class<IntIdEntity>>(),
        entityCacheWriter,
        entityCacheLoader,
        injector,
        scheduler,
        executor,
        settings,
        initializerProvider
      ).unsafeCast()
    } else {
      CHMEntityCache(
        entityClass,
        entityCacheWriter,
        entityCacheLoader,
        injector,
        scheduler,
        executor,
        settings,
        initializerProvider
      )
    }
  }
}
