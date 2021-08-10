package play.entity.cache

import com.typesafe.config.Config
import play.entity.Entity
import play.util.ClassUtil
import play.util.isAbstract
import java.time.Duration

interface EntityCacheFactory {

  fun <ID : Any, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E>
}

abstract class AbstractEntityCacheFactory(conf: Config) : EntityCacheFactory {
  protected val settings: Settings

  init {
    val initialCacheSize = conf.getInt("initial-size")
    val expireAfterAccess = conf.getDuration("expire-after-access")
    val persistInterval = conf.getDuration("persist-interval")
    val loadTimeout = conf.getDuration("load-timeout")
    settings = Settings(initialCacheSize, expireAfterAccess, persistInterval, loadTimeout)
  }

  fun checkEntityClass(entityClass: Class<out Entity<*>>) {
    check(!entityClass.isAbstract()) { "" }
    check(ClassUtil.isTopLevelConcreteClass(entityClass)) { "" }
  }

  class Settings(
    @JvmField val initialSize: Int,
    @JvmField val expireAfterAccess: Duration,
    @JvmField val persistInterval: Duration,
    @JvmField val loadTimeout: Duration
  )
}

