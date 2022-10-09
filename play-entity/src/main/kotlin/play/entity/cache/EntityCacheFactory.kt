package play.entity.cache

import com.typesafe.config.Config
import play.entity.Entity
import java.time.Duration

interface EntityCacheFactory {

  fun <ID, E : Entity<ID>> create(
    entityClass: Class<E>,
    initializerProvider: EntityInitializerProvider
  ): EntityCache<ID, E>

  class Settings(
    @JvmField val initialSize: Int,
    @JvmField val expireAfterAccess: Duration,
    @JvmField val persistInterval: Duration,
    @JvmField val loadTimeout: Duration
  ) {
    init {
      require(initialSize > 0) { "initialSize must be greater than 0" }
      require(expireAfterAccess.toSeconds() > 0) { "expireAfterAccess must be greater than 0s" }
      require(persistInterval.toSeconds() > 0) { "persistInterval must be greater than 0s" }
      require(loadTimeout.toSeconds() > 0) { "loadTimeout must be greater than 0s" }
    }

    companion object {
      @JvmStatic
      fun parseFromConfig(conf: Config): Settings {
        val initialCacheSize = conf.getInt("initial-size")
        val expireAfterAccess = conf.getDuration("expire-after-access")
        val persistInterval = conf.getDuration("persist-interval")
        val loadTimeout = conf.getDuration("load-timeout")
        return Settings(initialCacheSize, expireAfterAccess, persistInterval, loadTimeout)
      }
    }
  }
}

