package play.entity

import com.typesafe.config.Config
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.ShutdownCoordinator
import play.entity.cache.*
import play.inject.PlayInjector
import play.scheduling.Scheduler
import play.util.reflect.Reflect
import java.util.concurrent.Executor

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayEntityCacheConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun entityCacheFactory(
    writer: EntityCacheWriter,
    loader: EntityCacheLoader,
    injector: PlayInjector,
    scheduler: Scheduler,
    @Qualifier("dbExecutor") executor: Executor,
    config: Config
  ): EntityCacheFactory {
    val conf = config.getConfig("play.entity.cache")
    val impl = config.getString("play.entity.cache.impl")
    return Reflect.createInstance(impl, writer, loader, injector, scheduler, executor, conf)
  }

  @Bean
  @ConditionalOnMissingBean
  fun entityCachePersistFailOver(): EntityCachePersistFailOver = NOOPEntityCachePersistFailOver

  @Bean
  @ConditionalOnMissingBean
  fun entityCacheManager(
    factory: EntityCacheFactory,
    shutdownCoordinator: ShutdownCoordinator,
    injector: PlayInjector,
    persistFailOver: EntityCachePersistFailOver
  ): EntityCacheManager {
    return EntityCacheManagerImpl(factory, shutdownCoordinator, injector, persistFailOver)
  }
}
