package play.entity.cache

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Assertions
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

  private val myEntityCache = MyEntityCache(entityCacheManager, memoryRepository, scheduler)

  private val myObjIdEntityCache = MyObjIdEntityCache(entityCacheManager, memoryRepository, scheduler)

  @Test
  fun testMyEntity() {
    for (playerId in 1..10L) {
      for (i in 1..10L) {
        myEntityCache.getOrCreate(playerId * 100 + i, playerId)
      }
    }
    for (playerId in 1..10L) {
      val partitionEntities = myEntityCache.getByIndex(playerId)
      Assertions.assertEquals(10, partitionEntities.size)
      println("$playerId: $partitionEntities")
    }
  }

  @Test
  fun testMyObjIdEntity() {
    for (playerId in 1..10L) {
      for (i in 1..10) {
        myObjIdEntityCache.getOrCreate(MyObjId(playerId, i))
      }
    }
    for (playerId in 1..10L) {
      val partitionEntities = myObjIdEntityCache.getByIndex(playerId)
      Assertions.assertEquals(10, partitionEntities.size)
      println("$playerId: $partitionEntities")
    }
  }
}
