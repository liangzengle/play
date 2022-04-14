package play.example.game.app.module.servertask

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.Behaviors
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.container.gs.GameServerScopeConfiguration

@Configuration(proxyBeanMethods = false)
class ServerTaskSpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun serverTaskManager(
    taskService: ServerTaskService
  ): ActorRef<ServerTaskManager.Command> {
    return spawn("ServerTaskManager") {
      Behaviors.setup { ctx -> ServerTaskManager(ctx, taskService) }
    }
  }
}
