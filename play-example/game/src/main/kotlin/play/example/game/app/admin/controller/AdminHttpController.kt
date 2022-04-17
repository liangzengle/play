package play.example.game.app.admin.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.admin.AdminHttpActionManager
import play.example.game.container.gs.domain.GameServerId
import play.net.http.*

@Component
class AdminHttpController @Autowired constructor(actionManager: AdminHttpActionManager, private val gameServerId: GameServerId) :
  AbstractHttpController(actionManager) {

  @Get("/get")
  fun httpGet(v1: Int, v2: String): HttpResult {
    return ok("v1=$v1 v2=$v2")
  }

  @Post("/post")
  fun httpPost(httpRequest: AbstractHttpRequest): HttpResult {
    return ok("body=${httpRequest.getBodyAsString()}")
  }

  @Get("/get/{itemId}/name")
  fun httpGetVariable(httpRequest: AbstractHttpRequest, itemId: Int): HttpResult {
    return ok("name=$itemId")
  }

  @Get("/server/info")
  fun httpGetVariable(): HttpResult {
    return ok(gameServerId.toString())
  }
}
