package play.example.game.container.gs

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.common.akka.ActorConfigurationSupport
import play.example.common.akka.GuardianBehavior
import play.example.game.container.db.ContainerRepositoryProvider

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
    containerRepositoryProvider: ContainerRepositoryProvider
  ): ActorRef<GameServerManager.Command> {
    return spawn(
      actorSystem,
      Behaviors.setup { ctx -> GameServerManager(ctx, applicationContext, containerRepositoryProvider) },
      "gs"
    )
  }
}
