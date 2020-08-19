package play.example.game.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.entity.PlayEntityCacheConfiguration
import play.entity.cache.DefaultEntityCachePersistFailOver
import play.entity.cache.EntityCachePersistFailOver
import play.event.EnableGuavaEventBus
import play.example.game.container.command.CommandManager
import play.example.game.container.command.CommandService
import play.example.game.container.gs.domain.GameServerId
import play.inject.PlayInjector
import play.inject.SpringPlayInjector
import play.mongodb.PlayMongoRepositoryConfiguration

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Import(value = [PlayMongoRepositoryConfiguration::class, PlayEntityCacheConfiguration::class])
@EnableGuavaEventBus
@Configuration(proxyBeanMethods = false)
class GameApp {

  @Bean
  fun gmCommandService(injector: PlayInjector, invokerManager: CommandManager): CommandService {
    return CommandService(injector, invokerManager)
  }

  @Bean
  fun playInjector(applicationContext: ApplicationContext): PlayInjector {
    return SpringPlayInjector(applicationContext)
  }

  @Bean
  fun entityCachePersistFailOver(gameServerId: GameServerId): EntityCachePersistFailOver {
    return DefaultEntityCachePersistFailOver("entity_back_up/${gameServerId.toInt()}")
  }
}
