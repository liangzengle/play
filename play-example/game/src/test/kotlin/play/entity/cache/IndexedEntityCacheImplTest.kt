package play.entity.cache

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import play.db.memory.MemoryRepository
import play.entity.cache.chm.CHMEntityCacheFactory
import play.inject.NOOPPlayInjector
import play.scheduling.DefaultScheduler
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

/**
 * @author LiangZengle
 */
internal class IndexedEntityCacheImplTest {

  private val memoryRepository = MemoryRepository()
  private val scheduler =
    DefaultScheduler(Executors.newScheduledThreadPool(1), ForkJoinPool.commonPool(), Clock.systemDefaultZone())
  private val cacheConfig = ConfigFactory.parseString(
    """
      initial-size = 256
      expire-after-access = 1m
      persist-interval = 5m
      load-timeout = 5s
    """.trimMargin()
  )
  private val entityCacheFactory = CHMEntityCacheFactory(
    memoryRepository,
    memoryRepository,
    NOOPPlayInjector,
    scheduler,
    ForkJoinPool.commonPool(),
    EntityCacheFactory.Settings.parseFromConfig(cacheConfig)
  )
  private val entityCacheManager =
    EntityCacheManagerImpl(entityCacheFactory, NOOPPlayInjector, NOOPEntityCachePersistFailOver)

  private val entityCache = MyEntityCache(entityCacheManager, memoryRepository, scheduler)

  @Test
  fun getMulti() {
    for (playerId in 1..10L) {
      for (i in 1..10L) {
        entityCache.getOrCreate(i, playerId)
      }
    }
    for (playerId in 1..10L) {
      val partitionEntities = entityCache.getByIndex(playerId)
      println("$playerId: $partitionEntities")
    }
  }
}
