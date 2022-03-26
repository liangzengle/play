package play.example.game.container

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.PlayCoreConfiguration
import play.db.PlayDBConfiguration
import play.example.common.akka.ActorConfigurationSupport
import play.example.common.akka.AkkaConfiguration
import play.example.common.akka.GuardianBehavior
import play.example.common.net.NettyServerConfiguration
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.container.command.CommandManager
import play.example.game.container.login.LoginDispatcherActor
import play.example.game.container.net.SessionManager
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
class ContainerApp : ActorConfigurationSupport {
  @Bean
  fun loginDispatcher(
    actorSystem: ActorSystem<GuardianBehavior.Command>,
    sessionManager: ActorRef<SessionManager.Command>
  ): ActorRef<LoginDispatcherActor.Command> {
    return spawn(
      actorSystem,
      Behaviors.setup { ctx -> LoginDispatcherActor(ctx, sessionManager) },
      "LoginDispatcherActor"
    )
  }

  @Bean
  fun commandManager(classScanner: ClassScanner): CommandManager {
    return CommandManager(Self::class.java, classScanner)
  }
}
