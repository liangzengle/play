package play.rsocket.rpc

import play.rsocket.metadata.RoutingMetadata
import play.rsocket.serializer.ByteBufToIOStreamAdapter
import play.rsocket.serializer.PlaySerializerProvider
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 *
 * @author LiangZengle
 */
@Suppress("UNCHECKED_CAST")
class StubRpcClient(
  private val requester: AbstractRSocketRequester,
  private val ioStreamAdapter: ByteBufToIOStreamAdapter,
  private val serializerProvider: PlaySerializerProvider,
) : NodeAwareRpcClient() {

  private val serviceStubFactories = object : ClassValue<() -> RpcServiceStub>() {
    override fun computeValue(type: Class<*>): () -> RpcServiceStub {
      val stubClass = Class.forName(type.name + "Stub")
      val handle = MethodHandles.lookup().findConstructor(
        stubClass, MethodType.methodType(
          Void.TYPE,
          AbstractRSocketRequester::class.java,
          ByteBufToIOStreamAdapter::class.java,
          PlaySerializerProvider::class.java
        )
      )
      return ReflectFactory(requester, ioStreamAdapter, serializerProvider, handle)
    }
  }

  fun init(serviceInterfaces: Iterable<Class<*>>) {
    for (serviceInterface in serviceInterfaces) {
      val factory = serviceStubFactories.get(serviceInterface)
      // ensure factory works
      factory()
    }
  }

  override fun <T : Any> getRpcService0(serviceInterface: Class<T>, routing: RoutingMetadata): T {
    val service = serviceStubFactories.get(serviceInterface).invoke()
    service.routingMetadata(routing)
    return service as T
  }

  private class ReflectFactory(
    private val requester: AbstractRSocketRequester,
    private val ioStreamAdapter: ByteBufToIOStreamAdapter,
    private val serializerProvider: PlaySerializerProvider,
    private val handle: MethodHandle
  ) : () -> RpcServiceStub {
    override fun invoke(): RpcServiceStub =
      handle.invoke(requester, ioStreamAdapter, serializerProvider) as RpcServiceStub
  }
}
