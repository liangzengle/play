package play.example.common.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import com.typesafe.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.ShutdownCoordinator
import play.example.common.akka.scheduling.AkkaScheduler
import play.example.common.akka.scheduling.ActorScheduler
import play.scala.await
import play.scheduling.Scheduler
import java.time.Clock
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class AkkaConfiguration : ActorConfigurationSupport {

  @Bean
  fun actorSystem(conf: Config, shutdownCoordinator: ShutdownCoordinator): ActorSystem<GuardianBehavior.Command> {
    val system = ActorSystem.create(GuardianBehavior.behavior, conf.getString("play.actor-system-name"))
    shutdownCoordinator.addShutdownTask("Shutdown Actor System", system) {
      it.terminate()
      it.whenTerminated().await(Duration.ofMinutes(1))
    }
    return system
  }

  @Bean
  fun scheduler(actorSystem: ActorSystem<*>, clock: Clock): Scheduler {
    return AkkaScheduler(actorSystem.scheduler(), actorSystem.executionContext(), clock)
  }

  @Bean
  fun actorScheduler(
    actorSystem: ActorSystem<GuardianBehavior.Command>,
    scheduler: Scheduler
  ): ActorRef<ActorScheduler.Command> {
    return spawn(actorSystem, Behaviors.setup { ActorScheduler(it, scheduler) }, "ActorScheduler")
  }
}
