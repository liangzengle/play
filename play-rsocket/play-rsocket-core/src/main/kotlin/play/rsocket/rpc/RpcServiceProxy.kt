package play.rsocket.rpc

import io.netty.buffer.ByteBufAllocator
import io.rsocket.metadata.CompositeMetadataCodec
import io.rsocket.metadata.WellKnownMimeType
import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import play.rsocket.RequestType
import play.rsocket.serializer.ByteBufToIOStreamAdapter
import play.rsocket.serializer.RSocketSerializer
import play.rsocket.serializer.RSocketSerializerProvider
import reactor.core.publisher.Flux
import java.lang.reflect.Method

/**
 *
 * @author LiangZengle
 */
class RpcServiceProxy(
  private val serviceInterface: Class<*>,
  private val ioStreamAdapter: ByteBufToIOStreamAdapter,
  private val serializerProvider: RSocketSerializerProvider,
  private val requester: AbstractRSocketRequester
) {

  companion object {
    private val methodMetadataCache = object : ClassValue<Map<Method, RpcMethodMetadata>>() {
      override fun computeValue(type: Class<*>): Map<Method, RpcMethodMetadata> {
        return type.methods.asSequence().filterNot { it.isDefault }.map { it to RpcMethodMetadata.of(it) }.toMap()
      }
    }
  }

  @RuntimeType
  fun invoke(@This proxy: Any, @Origin method: Method, @AllArguments allArguments: Array<Any>): Any? {
    proxy as RoutingRpcServiceProxy
    val methodMetadata =
      methodMetadataCache[serviceInterface][method] ?: throw IllegalStateException("methodMetadata not exists: $method")
    val parameters = method.parameters
    val parameterCount =
      if (methodMetadata.requestType == RequestType.RequestChannel) parameters.size - 1 else parameters.size

    val data = ByteBufAllocator.DEFAULT.buffer(64)
    data.writeInt(methodMetadata.serviceId)
    data.writeInt(methodMetadata.methodId)
    if (parameterCount > 0) {
      val serializer = serializerProvider.get()
      val output = ioStreamAdapter.toOutputStream(data)
      for (i in 0 until parameterCount) {
        val parameter = parameters[i]
        val parameterValue = allArguments[i]
        RSocketSerializer.write(serializer, output, parameter.parameterizedType, parameterValue)
      }
    }
    val compositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer()
    CompositeMetadataCodec.encodeAndAddMetadata(
      compositeMetadata,
      ByteBufAllocator.DEFAULT,
      WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.string,
      proxy.routingMetadata.content
    )
    val publisher =
      if (methodMetadata.requestType == RequestType.RequestChannel) allArguments.last() as Flux<*> else null
    return requester.initiateRequest(proxy.routingMetadata, methodMetadata, data, publisher)
  }
}
