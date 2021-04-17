package play.entity.cache.caffeine

import com.google.inject.Injector
import com.typesafe.config.Config
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import play.entity.Entity
import play.entity.cache.*
import play.scheduling.Scheduler

/**
 * Factory for creating CaffeineEntityCache
 * @author LiangZengle
 */
class CaffeineEntityCacheFactory @Inject constructor(
  private val entityCacheWriter: EntityCacheWriter,
  private val entityCacheLoader: EntityCacheLoader,
  private val injector: Injector,
  private val scheduler: Scheduler,
  private val executor: Executor,
  @Named("entity") entityConf: Config
) : AbstractEntityCacheFactory(entityConf.getConfig("cache")) {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializer: EntityInitializer<E>
  ): EntityCache<ID, E> {
    checkEntityClass(entityClass)
    return CaffeineEntityCache(
      entityClass,
      entityCacheWriter,
      entityCacheLoader,
      injector,
      scheduler,
      executor,
      settings,
      initializer
    )
  }
}
