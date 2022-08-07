package play.example.game.container.net.message

import play.example.net.message.RequestParams
import play.mvc.RequestBody
import play.mvc.RequestBodyBuilder
import play.mvc.RequestBodyFactory

/**
 *
 * @author LiangZengle
 */
class WireRequestBodyFactory : RequestBodyFactory {
  override fun empty(): RequestBody {
    return WireRequestBody(RequestParams())
  }

  override fun builder(): RequestBodyBuilder {
    return WireRequestBodyBuilder()
  }

  override fun parseFrom(bytes: ByteArray): RequestBody {
    return WireRequestBody(RequestParams.ADAPTER.decode(bytes))
  }
}
