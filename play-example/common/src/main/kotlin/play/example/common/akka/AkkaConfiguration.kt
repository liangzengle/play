package play.example.common.akka

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Adapter
import com.typesafe.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.ShutdownCoordinator
import play.example.common.akka.scheduling.AkkaScheduler
import play.example.common.akka.scheduling.LightArrayRevolverScheduler
import play.scala.await
import play.scheduling.Scheduler
import java.time.Clock
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class AkkaConfiguration {

  @Bean
  fun actorSystem(
    conf: Config,
    shutdownCoordinator: ShutdownCoordinator,
    clock: Clock
  ): ActorSystem<GuardianBehavior.Command> {
    val system = ActorSystem.create(GuardianBehavior.behavior, conf.getString("play.actor-system-name"))
    shutdownCoordinator.addShutdownTask("Shutdown Actor System") {
      system.terminate()
      system.whenTerminated().await(Duration.ofMinutes(1))
    }
    val scheduler = Adapter.toClassic(system.scheduler())
    if (scheduler is LightArrayRevolverScheduler) {
      scheduler.setClock(clock)
    }
    return system
  }

  @Bean
  fun scheduler(actorSystem: ActorSystem<*>, clock: Clock): Scheduler {
    return AkkaScheduler(actorSystem.scheduler(), actorSystem.executionContext(), clock)
  }
}
