package play.rsocket.broker.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import org.reactivestreams.Publisher
import play.rsocket.broker.routing.RSocketLocator
import play.rsocket.metadata.RoutingMetadata
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
class RoutingRSocket(
  private val rSocketLocator: RSocketLocator,
  private val routingMetaDataExtractor: (Payload) -> RoutingMetadata
) : RSocket {

  override fun fireAndForget(payload: Payload): Mono<Void> {
    return try {
      locate(payload).fireAndForget(payload)
    } catch (e: Throwable) {
      payload.release()
      Mono.error(e)
    }
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    return try {
      locate(payload).requestResponse(payload)
    } catch (e: Throwable) {
      payload.release()
      Mono.error(e)
    }
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    return try {
      locate(payload).requestStream(payload)
    } catch (e: Throwable) {
      payload.release()
      Flux.error(e)
    }
  }

  override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
    return Flux.from(payloads).switchOnFirst { signal, flux ->
      if (!signal.hasValue()) {
        flux
      } else {
        val payload = signal.get()!!
        try {
          locate(payload).requestChannel(flux)
          flux
        } catch (e: Throwable) {
          payload.release()
          Flux.error(e)
        }
      }
    }
  }

  override fun metadataPush(payload: Payload): Mono<Void> {
    return try {
      locate(payload).metadataPush(payload)
    } catch (e: Throwable) {
      payload.release()
      Mono.error(e)
    }
  }

  private fun locate(payload: Payload): RSocket {
    val routingMetadata = routingMetaDataExtractor(payload)
    return rSocketLocator.locate(routingMetadata)
  }
}
