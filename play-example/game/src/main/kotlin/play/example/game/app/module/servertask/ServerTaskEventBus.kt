package play.example.game.app.module.servertask

import akka.actor.typed.ActorRef
import org.springframework.stereotype.Component
import play.example.game.app.module.servertask.event.ServerTaskEvent

@Component
class ServerTaskEventBus(private val serverTaskManager: ActorRef<ServerTaskManager.Command>) {

  fun post(event: ServerTaskEvent) {
    serverTaskManager.tell(event)
  }
}
