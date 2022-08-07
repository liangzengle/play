package play.rsocket.broker.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import org.reactivestreams.Publisher
import reactor.core.Disposables
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 * @author LiangZengle
 */
class MulticastRSocket(private val sockets: List<RSocket>) : RSocket {

  override fun fireAndForget(payload: Payload): Mono<Void> {
    if (sockets.isEmpty()) {
      payload.release()
      return Mono.empty()
    }
    if (sockets.size > 1) {
      payload.retain(sockets.size - 1)
    }
    return Flux.fromIterable(sockets)
      .flatMap { it.fireAndForget(payload) }
      .ignoreElements()
  }

  override fun requestResponse(payload: Payload): Mono<Payload> {
    if (sockets.isEmpty()) {
      payload.release()
      return Mono.empty()
    }
    if (sockets.size > 1) {
      payload.retain(sockets.size - 1)
    }
    return Mono.create { sink ->
      val compositeDisposable = Disposables.composite()
      sink.onDispose(compositeDisposable)
      sockets.asSequence()
        .map { it.requestResponse(payload).subscribe(sink::success, sink::error, sink::success) }
        .forEach(compositeDisposable::add)
    }
  }

  override fun requestStream(payload: Payload): Flux<Payload> {
    if (sockets.isEmpty()) {
      payload.release()
      return Flux.empty()
    }
    if (sockets.size > 1) {
      payload.retain(sockets.size - 1)
    }
    return Flux.fromIterable(sockets).flatMap { it.requestStream(payload) }
  }

  override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
    if (sockets.isEmpty()) {
      return Flux.empty()
    }
    val payloadPublisher: Publisher<Payload> = if (sockets.size > 1) {
      Flux.from(payloads).map { it.retain(sockets.size - 1) }
    } else {
      payloads
    }
    return Flux.fromIterable(sockets).flatMap { it.requestChannel(payloadPublisher) }
  }

  override fun metadataPush(payload: Payload): Mono<Void> {
    if (sockets.isEmpty()) {
      payload.release()
      return Mono.empty()
    }
    if (sockets.size > 1) {
      payload.retain(sockets.size - 1)
    }
    return Flux.fromIterable(sockets).flatMap { it.metadataPush(payload) }.ignoreElements()
  }
}
