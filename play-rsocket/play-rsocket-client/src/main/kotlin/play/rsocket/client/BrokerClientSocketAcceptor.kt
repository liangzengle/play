package play.rsocket.client

import io.rsocket.ConnectionSetupPayload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
class BrokerClientSocketAcceptor(private val responderFactory: (RSocket) -> RSocket) : SocketAcceptor {
  override fun accept(setup: ConnectionSetupPayload, sendingSocket: RSocket): Mono<RSocket> {
    return Mono.just(responderFactory(sendingSocket))
  }
}
