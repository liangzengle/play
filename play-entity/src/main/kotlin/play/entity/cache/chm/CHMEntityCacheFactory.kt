package play.entity.cache.chm

import com.typesafe.config.Config
import play.entity.Entity
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import java.util.concurrent.Executor

class CHMEntityCacheFactory(
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: PlayInjector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  cacheConf: Config
) : AbstractEntityCacheFactory(cacheConf) {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E> {
    return CHMEntityCache(
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
