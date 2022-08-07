package play.rsocket.rpc

import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCountUtil
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.exceptions.InvalidException
import io.rsocket.util.ByteBufPayload
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 *
 * @author LiangZengle
 */
abstract class AbstractRSocketResponder(
  protected val localServiceCallerRegistry: LocalServiceCallerRegistry,
  protected val resultEncoder: (Any) -> ByteBuf
) : RSocket {

  override fun fireAndForget(payload: Payload): Mono<Void> {
    return try {
      localFireAndForget(payload)
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(e)
    }
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    return try {
      localRequestResponse(payload)
    } catch (e: Throwable) {
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(e)
    }
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    return try {
      localRequestStream(payload)
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
          localRequestChannel(payload, flux.skip(1))
        } catch (e: Throwable) {
          ReferenceCountUtil.safeRelease(payload)
          Flux.error(e)
        }
      }
    }
  }

  abstract override fun metadataPush(payload: Payload): Mono<Void>

  @Suppress("UNCHECKED_CAST")
  fun localFireAndForget(payload: Payload): Mono<Void> {
    val result = invokeLocalService(payload, null) { Mono.error<Payload>(it) }
    return if (result is Mono<*>) result as Mono<Void> else Mono.empty()
  }

  fun localRequestResponse(payload: Payload): Mono<Payload> {
    val result = invokeLocalService(payload, null) { Mono.error<Payload>(it) }
    return (result as Mono<*>).map { ByteBufPayload.create(resultEncoder(it)) }
  }

  fun localRequestStream(payload: Payload): Flux<Payload> {
    val result = invokeLocalService(payload, null) { Flux.error<Payload>(it) }
    return (result as Flux<*>).map { ByteBufPayload.create(resultEncoder(it)) }
  }

  fun localRequestChannel(payload: Payload, publisher: Flux<Payload>): Flux<Payload> {
    val result = invokeLocalService(payload, publisher) { Flux.error<Payload>(it) }
    return (result as Flux<*>).map { ByteBufPayload.create(resultEncoder(it)) }
  }

  private fun invokeLocalService(
    payload: Payload,
    publisher: Flux<Payload>?,
    onError: (Throwable) -> Any
  ): Any? {
    val data = payload.data()
    val serviceId = data.readInt()
    val methodId = data.readInt()
    val service = localServiceCallerRegistry.find(serviceId)
      ?: return onError(InvalidException("Service Not Found: serviceId=$serviceId"))
    val result = service.call(methodId, data, publisher)
    if (result == LocalServiceCaller.NotFound) {
      return onError(InvalidException("Method Not Found: serviceId=$serviceId, methodId=$methodId"))
    }
    return result
  }
}
