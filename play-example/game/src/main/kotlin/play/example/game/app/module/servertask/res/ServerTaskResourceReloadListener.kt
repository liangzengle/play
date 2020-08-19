package play.example.game.app.module.servertask.res

import akka.actor.typed.ActorRef
import org.springframework.stereotype.Component
import play.example.game.app.module.servertask.ServerTaskManager
import play.res.GenericResourceReloadListener

@Component
class ServerTaskResourceReloadListener(
  private val serverTaskManager: ActorRef<ServerTaskManager.Command>
) : GenericResourceReloadListener<ServerTaskResource>() {
  override fun onReload() {
    serverTaskManager.tell(ServerTaskManager.ResourceReloaded)
  }
}
