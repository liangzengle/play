package play.rsocket.rpc

import com.alibaba.rsocket.cloudevents.CloudEventImpl
import com.alibaba.rsocket.metadata.GSVRoutingMetadata
import com.alibaba.rsocket.metadata.MessageAcceptMimeTypesMetadata
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata
import com.alibaba.rsocket.metadata.RSocketMimeType
import com.alibaba.rsocket.observability.RsocketErrorCode
import com.alibaba.rsocket.rpc.RSocketResponderHandler
import com.alibaba.rsocket.rpc.ReactiveMethodHandler
import io.netty.util.ReferenceCountUtil
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.exceptions.InvalidException
import io.rsocket.util.ByteBufPayload
import mu.KLogging
import play.util.unsafeCast
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.extra.processor.TopicProcessor

/**
 *
 * @author LiangZengle
 */
class RSocketResponderHandler(
  private val serviceCall: GSVLocalReactiveServiceCaller,
  eventProcessor: TopicProcessor<CloudEventImpl<*>>,
  requester: RSocket,
  setupPayload: ConnectionSetupPayload,
) : RSocketResponderHandler(serviceCall, eventProcessor, requester, setupPayload) {

  companion object : KLogging()

  override fun localRequestResponse(
    routing: GSVRoutingMetadata,
    dataEncodingMetadata: MessageMimeTypeMetadata,
    messageAcceptMimeTypesMetadata: MessageAcceptMimeTypesMetadata?,
    payload: Payload?
  ): Mono<Payload> {
    return try {
      val methodHandler = serviceCall.getInvokeMethod(routing.group, routing.version, routing.service, routing.method)
      if (methodHandler != null) {
        val result: Any? = if (methodHandler.isAsyncReturn) {
          invokeLocalService(methodHandler, dataEncodingMetadata, payload)
        } else {
          invokeLocalServiceSync(methodHandler, dataEncodingMetadata, payload)
        }
        //composite data for return value
        val resultEncodingType =
          resultEncodingType(messageAcceptMimeTypesMetadata, dataEncodingMetadata.rSocketMimeType, methodHandler)
        val monoResult: Mono<Any> = if (result is Mono<*>) {
          result.unsafeCast()
        } else {
          methodHandler.reactiveAdapter.toMono(result)
        }
        monoResult.map { encodingFacade.encodingResult(it, resultEncodingType) }.map {
          ByteBufPayload.create(it, getCompositeMetadataWithEncoding(resultEncodingType.type))
        }
      } else {
        ReferenceCountUtil.safeRelease(payload)
        Mono.error(InvalidException(RsocketErrorCode.message("RST-201404", routing.service, routing.method)))
      }
    } catch (e: Exception) {
      log.error(RsocketErrorCode.message("RST-200500"), e)
      ReferenceCountUtil.safeRelease(payload)
      Mono.error(InvalidException(RsocketErrorCode.message("RST-900500", e.message)))
    }
  }

  private fun invokeLocalServiceSync(
    methodHandler: ReactiveMethodHandler, dataEncodingMetadata: MessageMimeTypeMetadata, payload: Payload?
  ): Mono<Any?> {
    return Mono.create { sink: MonoSink<Any?> ->
      try {
        when (val resultObj = invokeLocalService(methodHandler, dataEncodingMetadata, payload)) {
          null -> sink.success()
          is Mono<*> -> {
            resultObj.doOnError { throwable: Throwable -> sink.error(throwable) }
              .doOnNext { t: Any? -> sink.success(t) }.thenEmpty(Mono.fromRunnable { sink.success() }).subscribe()
          }
          else -> sink.success(resultObj)
        }
      } catch (e: Exception) {
        sink.error(e)
      }
    }
  }

  private fun resultEncodingType(
    messageAcceptMimeTypesMetadata: MessageAcceptMimeTypesMetadata?,
    defaultEncodingType: RSocketMimeType,
    reactiveMethodHandler: ReactiveMethodHandler
  ): RSocketMimeType {
    if (reactiveMethodHandler.isBinaryReturn) {
      return RSocketMimeType.Binary
    }
    if (messageAcceptMimeTypesMetadata != null) {
      val firstAcceptType = messageAcceptMimeTypesMetadata.firstAcceptType
      if (firstAcceptType != null) {
        return firstAcceptType
      }
    }
    return defaultEncodingType
  }
}
