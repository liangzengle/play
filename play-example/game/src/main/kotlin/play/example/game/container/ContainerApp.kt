package play.example.game.container

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.PlayCoreConfiguration
import play.db.PlayDBConfiguration
import play.example.common.akka.AkkaConfiguration
import play.example.common.net.NettyServerConfiguration
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.container.command.CommandManager
import play.http.EnableHttpClient
import play.mongodb.PlayMongoConfiguration
import play.rsocket.rpc.RSocketRpcConfiguration
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
@EnableHttpClient
@Import(
  value = [
    PlayCoreConfiguration::class,
    PlayDBConfiguration::class,
    PlayMongoConfiguration::class,
    NettyServerConfiguration::class,
    AkkaConfiguration::class,
    RSocketRpcConfiguration::class
  ]
)
class ContainerApp {

  @Bean
  fun commandManager(classScanner: ClassScanner): CommandManager {
    return CommandManager(Self::class.java, classScanner)
  }
}
