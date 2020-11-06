package play.net.http

import com.google.common.net.HostAndPort
import play.util.json.Json
import java.util.*

interface BasicHttpRequest {
  fun uri(): String
  fun path(): String
  fun method(): String
  fun remoteAddress(): HostAndPort
  fun remoteHost(): String
  fun remotePost(): Int
  fun getHeader(name: String): Optional<String>
}

abstract class AbstractHttpRequest : BasicHttpRequest {
  abstract fun getBodyAsString(): String
  inline fun <reified T> getBodyAs() = Json.to<T>(getBodyAsString())
  abstract fun parameters(): HttpParameters
  abstract fun hasBody(): Boolean
}
