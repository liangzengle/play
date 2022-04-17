package play.entity.cache.caffeine

import com.typesafe.config.Config
import play.entity.Entity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import java.util.concurrent.Executor

/**
 * Factory for creating CaffeineEntityCache
 * @author LiangZengle
 */
class CaffeineEntityCacheFactory constructor(
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val settings: EntityCacheFactory.Settings
) : EntityCacheFactory {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E> {
    return CaffeineEntityCache(
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
