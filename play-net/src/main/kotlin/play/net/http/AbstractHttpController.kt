package play.net.http

import io.vavr.concurrent.Future
import io.vavr.control.Option
import play.util.collection.EmptyByteArray

abstract class AbstractHttpController {

  protected fun ok(result: String): HttpResult = HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(result))

  protected fun ok(result: ByteArray): HttpResult = HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(result))

  protected fun ok(): HttpResult =
    HttpResult.Strict(HttpStatusCode.OK, HttpEntity.Strict(EmptyByteArray, Option.none()))

  protected fun error(statusCode: Int) = HttpResult.Strict(statusCode, HttpEntity.Strict(EmptyByteArray, Option.none()))

  protected fun Future<HttpResult.Strict>.toHttpResult() = HttpResult.Lazy(this)
}
