package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.Repository
import play.akka.ActorConfigurationSupport
import play.akka.GuardianBehavior
import play.example.game.container.login.LoginDispatcherActor
import play.example.game.container.net.SessionManager

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class GameServerManagerConfiguration : ActorConfigurationSupport {

  @Bean
  fun gameServerManager(
    actorSystem: ActorSystem<GuardianBehavior.Command>,
    applicationContext: ConfigurableApplicationContext,
    repository: Repository
  ): ActorRef<GameServerManager.Command> {
    return spawn(
      actorSystem,
      Behaviors.setup { ctx ->
        GameServerManager(
          ctx,
          applicationContext,
          repository
        )
      },
      "GameServerManager"
    )
  }

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
}
