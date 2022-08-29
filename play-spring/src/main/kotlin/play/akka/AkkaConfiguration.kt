package play.akka

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.GracefullyShutdown
import play.akka.scheduling.AkkaScheduler
import play.scala.toPlay
import play.scheduling.Scheduler
import java.time.Clock

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class AkkaConfiguration : ActorConfigurationSupport {

  @Bean
  fun actorSystem(conf: Config, shutdown: GracefullyShutdown): ActorSystem<GuardianBehavior.Command> {
    val system = ActorSystem.create(GuardianBehavior.behavior, conf.getString("play.actor-system-name"))
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_ACTOR_SYSTEM,
      GracefullyShutdown.PHASE_SHUTDOWN_ACTOR_SYSTEM,
      system
    ) {
      it.terminate()
      it.whenTerminated().toPlay()
    }
    return system
  }

  @Bean
  fun scheduler(actorSystem: ActorSystem<*>, clock: Clock): Scheduler {
    return AkkaScheduler(actorSystem.scheduler(), actorSystem.executionContext(), clock)
  }
}
