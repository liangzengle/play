package play.db.cache

import play.Configuration
import play.db.*
import play.inject.Injector
import play.util.scheduling.Scheduler
import java.time.Duration
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface EntityCacheFactory {

  fun <ID : Any, E : Entity<ID>> create(entityClass: Class<E>, entityProcessor: EntityProcessor<E>): EntityCache<ID, E>
}

@Singleton
class DefaultEntityCacheFactory @Inject constructor(
  private val persistService: PersistService,
  private val queryService: QueryService,
  private val scheduler: Scheduler,
  private val executor: DbExecutor,
  private val injector: Injector,
  @Named("cache") conf: Configuration
) : EntityCacheFactory {

  private val config: Config

  init {
    val initialCacheSize = conf.getInt("initial-size")
    val expireAfterAccess = conf.getDuration("expire-after-access")
    val persistInterval = conf.getDuration("persist-interval")
    config = Config(initialCacheSize, expireAfterAccess, persistInterval)
  }

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    entityProcessor: EntityProcessor<E>
  ): EntityCache<ID, E> {
    return EntityCacheImpl(
      entityClass,
      persistService,
      queryService,
      injector,
      scheduler,
      executor,
      config,
      entityProcessor
    )
  }

  class Config(
    @JvmField val initialSize: Int,
    @JvmField val expireAfterAccess: Duration,
    @JvmField val persistInterval: Duration
  )

}
