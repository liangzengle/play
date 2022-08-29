package play.example.game.app.module.servertask

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.Behaviors
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.container.gs.GameServerScopeConfiguration
import play.util.classOf

@Configuration(proxyBeanMethods = false)
class ServerTaskSpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun serverTaskManager(
    taskService: ServerTaskService
  ): ActorRef<ServerTaskManager.Command> {
    return spawn("ServerTaskManager", classOf()) {
      Behaviors.setup { ctx -> ServerTaskManager(ctx, taskService) }
    }
  }
}
