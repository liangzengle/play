package play.net.netty.http

import com.google.common.net.HostAndPort
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import io.vavr.control.Option
import io.vavr.kotlin.option
import play.net.http.AbstractHttpRequest
import play.net.http.BasicHttpRequest
import play.net.http.HttpParameters

class BasicNettyHttpRequest internal constructor(
  val id: Long,
  val toNetty: FullHttpRequest,
  private val remoteAddress: HostAndPort
) : BasicHttpRequest {
  override fun uri(): String = toNetty.uri()

  override fun path(): String = QueryStringDecoder(toNetty.uri()).path()

  override fun method(): String = toNetty.method().name()

  override fun remoteAddress(): HostAndPort = remoteAddress

  override fun remoteHost(): String = remoteAddress.host

  override fun remotePost(): Int = remoteAddress.port

  override fun getHeader(name: String): Option<String> = toNetty.headers().get(name).option()

  override fun toString(): String {
    return "$id ${method()} ${uri()} ${toNetty.protocolVersion()} ${remoteHost()}:${remotePost()}"
  }
}

class NettyHttpRequest internal constructor(
  private val basic: BasicNettyHttpRequest,
  private val body: String?,
  private val parameters: NettyHttpParameters
) : AbstractHttpRequest(), BasicHttpRequest by basic {

  override fun hasBody(): Boolean = body != null

  override fun getBodyAsString(): String {
    return body ?: throw NoSuchElementException("No Request Body.")
  }

  override fun parameters(): HttpParameters = parameters
  override fun toString(): String {
    val b = StringBuilder(256)
    b.append(basic).append('\n')
    if (body != null) {
      b.append("body=").append(body).append('\n')
    }
    b.append(parameters)
    return b.toString()
  }
}
