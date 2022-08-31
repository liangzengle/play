package play.rsocket.client

import io.rsocket.RSocket
import play.rsocket.rpc.AbstractRSocketRequester
import play.rsocket.serializer.RSocketCodec

/**
 *
 *
 * @author LiangZengle
 */
class ClientRSocketRequester(private val socket: () -> RSocket, codec: RSocketCodec) : AbstractRSocketRequester(codec) {
  override fun upstreamRSocket(): RSocket {
    return socket()
  }
}
