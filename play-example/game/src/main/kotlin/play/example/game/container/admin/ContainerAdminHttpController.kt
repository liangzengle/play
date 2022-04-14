package play.example.game.container.admin

import akka.actor.typed.ActorRef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.container.gs.GameServerManager
import play.net.http.AbstractHttpController
import play.net.http.HttpResult
import play.net.http.Route
import play.util.concurrent.PlayPromise

/**
 * @author LiangZengle
 */
@Component
class ContainerAdminHttpController @Autowired constructor(
  controllerManager: ContainerAdminHttpActionManager,
  private val gameServerManager: ActorRef<GameServerManager.Command>,
) : AbstractHttpController(controllerManager) {

  @Route("/deploy")
  fun deploy(serverId: Int): HttpResult {
    val promise = PlayPromise.make<Int>()
    gameServerManager.tell(GameServerManager.CreateGameServer(serverId, promise))
    return promise.future.map { ok("""{"code": $it}""") }.toHttpResult()
  }

  @Route("/start")
  fun start(serverId: Int): HttpResult {
    val promise = PlayPromise.make<Int>()
    gameServerManager.tell(GameServerManager.StartGameServer(serverId, promise))
    return promise.future.map { ok("""{"code": $it}""") }.toHttpResult()
  }

  @Route("/close")
  fun close(serverId: Int): HttpResult {
    val promise = PlayPromise.make<Int>()
    gameServerManager.tell(GameServerManager.CloseGameServer(serverId, promise))
    return promise.future.map { ok("""{"code": $it}""") }.toHttpResult()
  }
}
