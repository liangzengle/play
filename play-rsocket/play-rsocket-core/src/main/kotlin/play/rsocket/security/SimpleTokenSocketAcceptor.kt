package play.rsocket.security

import io.rsocket.ConnectionSetupPayload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.exceptions.InvalidException
import io.rsocket.metadata.WellKnownMimeType
import play.rsocket.metadata.MetadataExtractor
import reactor.core.publisher.Mono

/**
 *
 *
 * @author LiangZengle
 */
class SimpleTokenSocketAcceptor(private val delegate: SocketAcceptor, private val token: String) : SocketAcceptor {
  override fun accept(setup: ConnectionSetupPayload, sendingSocket: RSocket): Mono<RSocket> {
    val metadata = MetadataExtractor.extract(
      setup,
      WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string,
      SimpleTokenMetadata.Default::parseFrom
    )
    if (metadata == null || metadata.token != token) {
      return Mono.error(InvalidException("Invalid token"))
    }
    return delegate.accept(setup, sendingSocket)
  }
}
