package play.rsocket.broker.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.exceptions.InvalidException
import org.reactivestreams.Publisher
import play.rsocket.metadata.RoutingMetadata
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
class UnknownRSocket(private val routingMetadata: RoutingMetadata) : RSocket {

  override fun fireAndForget(payload: Payload): Mono<Void> {
    payload.release()
    return Mono.error(InvalidException("Route not found: $routingMetadata"))
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    payload.release()
    return Mono.error(InvalidException("Route not found: $routingMetadata"))
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    payload.release()
    return Flux.error(InvalidException("Route not found: $routingMetadata"))
  }

  override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
    return Flux.error(InvalidException("Route not found: $routingMetadata"))
  }

  override fun metadataPush(payload: Payload): Mono<Void> {
    payload.release()
    return Mono.error(InvalidException("Route not found: $routingMetadata"))
  }
}
