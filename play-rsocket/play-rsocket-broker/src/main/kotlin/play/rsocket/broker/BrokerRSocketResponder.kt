package play.rsocket.broker

import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCountUtil
import io.rsocket.Payload
import io.rsocket.exceptions.InvalidException
import org.reactivestreams.Publisher
import play.rsocket.broker.routing.RSocketLocator
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.rpc.AbstractRSocketResponder
import play.rsocket.rpc.LocalServiceCallerRegistry
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 *
 * @author LiangZengle
 */
class BrokerRSocketResponder(
  private val brokerId: Int,
  private val rSocketLocator: RSocketLocator,
  private val routingMetadataExtractor: (Payload) -> RoutingMetadata?,
  localServiceCallerRegistry: LocalServiceCallerRegistry,
  resultEncoder: (Any) -> ByteBuf
) : AbstractRSocketResponder(localServiceCallerRegistry, resultEncoder) {

  override fun fireAndForget(payload: Payload): Mono<Void> {
    return try {
      val routing = routingMetadataExtractor(payload)
      if (routing == null) {
        ReferenceCountUtil.safeRelease(payload)
        Mono.error(InvalidException("RoutingMetadata Not Found"))
      } else {
        if (shouldResponse(routing)) {
          localFireAndForget(payload)
        } else {
          rSocketLocator.locate(routing).fireAndForget(payload)
        }
      }
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(e)
    }
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    return try {
      val routing = routingMetadataExtractor(payload)
      if (routing == null) {
        ReferenceCountUtil.safeRelease(payload)
        Mono.error(InvalidException("RoutingMetadata Not Found"))
      } else {
        if (shouldResponse(routing)) {
          localRequestResponse(payload)
        } else {
          rSocketLocator.locate(routing).requestResponse(payload)
        }
      }
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(e)
    }
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    return try {
      val routing = routingMetadataExtractor(payload)
      if (routing == null) {
        ReferenceCountUtil.safeRelease(payload)
        Flux.error(InvalidException("RoutingMetadata Not Found"))
      } else {
        if (shouldResponse(routing)) {
          localRequestStream(payload)
        } else {
          rSocketLocator.locate(routing).requestStream(payload)
        }
      }
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
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
          val routing = routingMetadataExtractor(payload) ?: throw InvalidException("RoutingMetadata Not Found")
          if (shouldResponse(routing)) {
            localRequestChannel(payload, flux)
          } else {
            rSocketLocator.locate(routing).requestChannel(flux)
          }
        } catch (e: Throwable) {
          ReferenceCountUtil.safeRelease(payload)
          Flux.error(e)
        }
      }
    }
  }

  override fun metadataPush(payload: Payload): Mono<Void> {
    return try {
      val routing =
        routingMetadataExtractor(payload) ?: return Mono.error(InvalidException("RoutingMetadata Not Found"))
      rSocketLocator.locate(routing).metadataPush(payload)
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(e)
    }
  }

  private fun shouldResponse(routing: RoutingMetadata): Boolean {
    return routing.nodeId == brokerId
  }
}
