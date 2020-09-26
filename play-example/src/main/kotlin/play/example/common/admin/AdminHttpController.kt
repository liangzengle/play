package play.example.common.admin

import play.net.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminHttpController @Inject constructor(actionManager: AdminHttpActionManager) :
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

}
