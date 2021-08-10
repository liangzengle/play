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
import play.example.game.container.login.LoginDispatcherActor
import play.example.game.container.net.SessionManager
import play.mongodb.PlayMongoConfiguration

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
@Import(
  value = [
    PlayCoreConfiguration::class,
    PlayDBConfiguration::class,
    PlayMongoConfiguration::class,
    NettyServerConfiguration::class,
    AkkaConfiguration::class
  ]
)
class ContainerApp : ActorConfigurationSupport {
  @Bean
  fun loginDispatcher(
    actorSystem: ActorSystem<GuardianBehavior.Command>,
    sessionManager: ActorRef<SessionManager.Command>
  ): ActorRef<LoginDispatcherActor.Command> {
    return spawn(actorSystem, Behaviors.setup { ctx -> LoginDispatcherActor(ctx, sessionManager) }, "loginDispatcher")
  }
}
