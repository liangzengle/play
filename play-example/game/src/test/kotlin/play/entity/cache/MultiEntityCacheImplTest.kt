package play.entity.cache

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test

import play.DefaultShutdownCoordinator
import play.db.memory.MemoryRepository
import play.entity.cache.chm.CHMEntityCacheFactory
import play.inject.NOOPPlayInjector
import play.scheduling.DefaultScheduler
import play.util.unsafeCast
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

/**
 * @author LiangZengle
 */
internal class MultiEntityCacheImplTest {

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
    cacheConfig
  )
  private val shutdownCoordinator = DefaultShutdownCoordinator()
  private val entityCacheManager =
    EntityCacheManagerImpl(entityCacheFactory, shutdownCoordinator, NOOPPlayInjector, NOOPEntityCachePersistFailOver)

  private val entityCache = entityCacheManager.get(MyEntity::class.java)
  private val partitionEntityCache = MultiEntityCacheImpl<Long, MyObjId, MyEntity>(entityCache, memoryRepository, scheduler, DefaultMultiCacheExpireEvaluator.unsafeCast())

  @Test
  fun getMulti() {
    for (playerId in 1..10L) {
      for (i in 1..10) {
        partitionEntityCache.getOrCreate(MyObjId(playerId, i), ::MyEntity)
      }
    }
    for (playerId in 1..10L) {
      val partitionEntities = partitionEntityCache.getMulti(playerId)
      println("$playerId: $partitionEntities")
    }
  }
}
