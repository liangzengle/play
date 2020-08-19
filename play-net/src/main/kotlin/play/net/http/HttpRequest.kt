package play.net.http

import com.google.common.net.HostAndPort
import io.vavr.control.Option
import play.util.json.Json

interface BasicHttpRequest {
  fun uri(): String
  fun path(): String
  fun method(): String
  fun remoteAddress(): HostAndPort
  fun remoteHost(): String
  fun remotePost(): Int
  fun getHeader(name: String): Option<String>
}

abstract class AbstractHttpRequest : BasicHttpRequest {
  abstract fun getBodyAsString(): String
  inline fun <reified T> getBodyAs() = Json.to<T>(getBodyAsString())
  abstract fun parameters(): HttpParameters

}
