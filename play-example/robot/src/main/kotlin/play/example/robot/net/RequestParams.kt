package play.example.robot.net

import play.mvc.RequestBody

abstract class RequestParams {

  abstract fun toRequestBody(): RequestBody
}
