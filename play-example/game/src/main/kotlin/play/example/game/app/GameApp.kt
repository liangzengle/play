package play.example.game.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.entity.PlayEntityCacheConfiguration
import play.example.game.container.gm.GmCommandService
import play.inject.PlayInjector
import play.inject.SpringPlayInjector
import play.mongodb.PlayMongoRepositoryConfiguration

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Import(value = [PlayMongoRepositoryConfiguration::class, PlayEntityCacheConfiguration::class])
@Configuration(proxyBeanMethods = false)
class GameApp {

  @Bean
  fun gmCommandService(injector: PlayInjector): GmCommandService {
    return GmCommandService(injector)
  }

  @Bean
  private fun playInjector(applicationContext: ApplicationContext): PlayInjector {
    return SpringPlayInjector(applicationContext)
  }
}
