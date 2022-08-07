package play.rsocket.client

import io.netty.buffer.ByteBuf
import io.rsocket.RSocket
import play.rsocket.rpc.AbstractRSocketRequester
import java.lang.reflect.Type

/**
 *
 *
 * @author LiangZengle
 */
class ClientRSocketRequester(
  private val socket: () -> RSocket,
  resultDecoder: (ByteBuf, Type) -> Any?,
  paramEncoder: (Any) -> ByteBuf
) : AbstractRSocketRequester(resultDecoder, paramEncoder) {
  override fun upstreamRSocket(): RSocket {
    return socket()
  }
}
