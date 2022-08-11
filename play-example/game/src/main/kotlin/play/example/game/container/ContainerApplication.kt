package play.example.game.container

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.PlayCoreConfiguration
import play.akka.EnableAkka
import play.db.PlayDBConfiguration
import play.example.common.net.NettyServerConfiguration
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.container.command.CommandManager
import play.http.EnableHttpClient
import play.mongodb.PlayMongoClientConfiguration
import play.rsocket.rpc.RSocketRpcConfiguration
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
@EnableHttpClient
@EnableAkka
@Import(
  value = [
    PlayCoreConfiguration::class,
    PlayDBConfiguration::class,
    PlayMongoClientConfiguration::class,
    NettyServerConfiguration::class,
    RSocketRpcConfiguration::class
  ]
)
class ContainerApplication {

  @Bean
  fun commandManager(classScanner: ClassScanner): CommandManager {
    return CommandManager(Self::class.java, classScanner)
  }
}
