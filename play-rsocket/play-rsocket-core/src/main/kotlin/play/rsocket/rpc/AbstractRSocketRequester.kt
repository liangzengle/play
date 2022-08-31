package play.rsocket.rpc

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.util.ByteBufPayload
import play.rsocket.RequestType
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.serializer.RSocketCodec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Type

/**
 *
 *
 * @author LiangZengle
 */
abstract class AbstractRSocketRequester(private val codec: RSocketCodec) {

  abstract fun upstreamRSocket(): RSocket

  @Suppress("UNCHECKED_CAST")
  fun <T> initiateRequest(
    routing: RoutingMetadata, methodMetadata: RpcMethodMetadata, data: ByteBuf, publisher: Flux<*>?
  ): T {
    val compositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer()
    CompositeMetadataCodec.encodeAndAddMetadata(
      compositeMetadata, ByteBufAllocator.DEFAULT, WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.string, routing.content
    )

    val result = when (methodMetadata.requestType) {
      RequestType.FireAndForget -> remoteFireAndForget(data, compositeMetadata)
      RequestType.RequestResponse -> remoteRequestResponse(data, compositeMetadata, methodMetadata.returnDataType)
      RequestType.RequestStream -> remoteRequestStream(data, compositeMetadata, methodMetadata.returnDataType)
      RequestType.RequestChannel -> remoteRequestChannel(
        data, compositeMetadata, methodMetadata.returnDataType, publisher!!
      )
    }
    return result as T
  }

  fun remoteFireAndForget(data: ByteBuf, compositeMetadata: ByteBuf): Mono<Void> {
    val payload = ByteBufPayload.create(data, compositeMetadata)
    val mono = upstreamRSocket().fireAndForget(payload)
    //
    mono.subscribe()
    return mono
  }

  fun remoteRequestResponse(data: ByteBuf, compositeMetadata: ByteBuf, returnDataType: Type): Mono<*> {
    val payload = ByteBufPayload.create(data, compositeMetadata)
    return upstreamRSocket().requestResponse(payload).map { p ->
      codec.decode(p.data(), returnDataType)
    }
  }

  fun remoteRequestStream(data: ByteBuf, compositeMetadata: ByteBuf, returnDataType: Type): Flux<*> {
    val payload = ByteBufPayload.create(data, compositeMetadata)
    val flux = upstreamRSocket().requestStream(payload)
    return flux.map { p ->
      codec.decode(p.data(), returnDataType)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun remoteRequestChannel(
    data: ByteBuf, compositeMetadata: ByteBuf, returnDataType: Type, publisher: Flux<*>
  ): Flux<*> {
    val firstPayload = ByteBufPayload.create(data, compositeMetadata)
    val payloads = (publisher as Flux<Any>).startWith(firstPayload).map {
      if (it is Payload) it else ByteBufPayload.create(codec.encode(it))
    }
    val flux = upstreamRSocket().requestChannel(payloads)
    return flux.map { p ->
      codec.decode(p.data(), returnDataType)
    }
  }
}
