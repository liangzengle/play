package play.db.cache

import play.Configuration
import play.db.*
import play.inject.Injector
import play.util.reflect.isAssignableFrom
import play.util.scheduling.Scheduler
import play.util.unsafeCast
import java.time.Duration
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface EntityCacheFactory {

  fun <ID : Any, E : Entity<ID>> create(entityClass: Class<E>, entityProcessor: EntityProcessor<E>): EntityCache<ID, E>
}

abstract class AbstractEntityCacheFactory(conf: Configuration) : EntityCacheFactory {
  protected val config: Config

  init {
    val initialCacheSize = conf.getInt("initial-size")
    val expireAfterAccess = conf.getDuration("expire-after-access")
    val persistInterval = conf.getDuration("persist-interval")
    config = Config(initialCacheSize, expireAfterAccess, persistInterval)
  }

  class Config(
    @JvmField val initialSize: Int,
    @JvmField val expireAfterAccess: Duration,
    @JvmField val persistInterval: Duration
  )
}

@Singleton
class DefaultEntityCacheFactory @Inject constructor(
  private val persistService: PersistService,
  private val queryService: QueryService,
  private val scheduler: Scheduler,
  private val executor: DbExecutor,
  private val injector: Injector,
  @Named("cache") conf: Configuration
) : AbstractEntityCacheFactory(conf) {

  override fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    entityProcessor: EntityProcessor<E>
  ): EntityCache<ID, E> {
    return when {
      isAssignableFrom<EntityLong>(entityClass) -> {
        EntityCacheLongImpl(
          entityClass.unsafeCast<Class<EntityLong>>(),
          persistService,
          queryService,
          injector,
          scheduler,
          executor,
          config,
          entityProcessor.unsafeCast()
        ).unsafeCast()
      }
      isAssignableFrom<EntityInt>(entityClass) -> {
        EntityCacheIntImpl(
          entityClass.unsafeCast<Class<EntityInt>>(),
          persistService,
          queryService,
          injector,
          scheduler,
          executor,
          config,
          entityProcessor.unsafeCast()
        ).unsafeCast()
      }
      else -> {
        EntityCacheImpl(
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
    }
  }
}
