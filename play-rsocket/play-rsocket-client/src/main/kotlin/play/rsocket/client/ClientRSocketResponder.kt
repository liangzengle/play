package play.rsocket.client

import io.netty.buffer.ByteBuf
import io.rsocket.Payload
import play.rsocket.rpc.AbstractRSocketResponder
import play.rsocket.rpc.LocalServiceCallerRegistry
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

/**
 *
 *
 * @author LiangZengle
 */
class ClientRSocketResponder(
  localServiceCallerRegistry: LocalServiceCallerRegistry,
  resultEncoder: (Any) -> ByteBuf,
  private val metadataPushSink: Sinks.Many<ByteBuf>
) : AbstractRSocketResponder(localServiceCallerRegistry, resultEncoder) {

  override fun metadataPush(payload: Payload): Mono<Void> {
    return Mono.fromRunnable { metadataPushSink.emitNext(payload.data(), Sinks.EmitFailureHandler.FAIL_FAST) }
  }
}
