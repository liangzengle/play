package play.example.common.admin

import play.net.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminHttpController @Inject constructor(actionManager: AdminHttpActionManager) : AbstractHttpController(actionManager) {

  @Get("/get")
  fun httpGet(httpRequest: AbstractHttpRequest, v1: Int, v2: String): HttpResult {
    return ok("")
  }

  @Post("/post")
  fun httpPost(): HttpResult {
    return ok("")
  }
}
