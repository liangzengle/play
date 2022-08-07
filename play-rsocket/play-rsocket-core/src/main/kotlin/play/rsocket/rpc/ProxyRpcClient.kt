package play.rsocket.rpc

import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.serializer.ByteBufToIOStreamAdapter
import play.rsocket.serializer.PlaySerializerProvider
import java.util.function.Function

/**
 *
 * @author LiangZengle
 */
class ProxyRpcClient(
  private val requester: AbstractRSocketRequester,
  private val ioStreamAdapter: ByteBufToIOStreamAdapter,
  private val serializerProvider: PlaySerializerProvider,
) : NodeAwareRpcClient() {

  private val proxyFactoryCache = object : ClassValue<Function<RoutingMetadata, Any>>() {
    override fun computeValue(type: Class<*>): Function<RoutingMetadata, Any> {
      return createProxyFactory(type)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun createProxyFactory(serviceInterface: Class<*>): Function<RoutingMetadata, Any> {
    val proxy = RpcServiceProxy(serviceInterface, ioStreamAdapter, serializerProvider, requester)
    val proxyDynamicType = ByteBuddy(ClassFileVersion.JAVA_V8)
      .subclass(RoutingRpcServiceProxy::class.java, ConstructorStrategy.Default.IMITATE_SUPER_CLASS)
      .name("${serviceInterface.name}Proxy")
      .implement(serviceInterface)
      .method(
        ElementMatchers.not(ElementMatchers.isDefaultMethod())
          .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Any::class.java)))
      )
      .intercept(MethodDelegation.to(proxy))
      .make()

    val constructor = proxyDynamicType
      .load(serviceInterface.classLoader, ClassLoadingStrategy.Default.INJECTION)
      .loaded
      .getConstructor(RoutingMetadata::class.java)


    val proxyFactoryDynamicType = ByteBuddy(ClassFileVersion.JAVA_V8)
      .subclass(
        TypeDescription.Generic.Builder.parameterizedType(
          Function::class.java,
          RoutingMetadata::class.java,
          serviceInterface
        ).build()
      )
      .name("${serviceInterface.name}ProxyFactory")
      .method(ElementMatchers.named("apply"))
      .intercept(MethodCall.construct(constructor).withAllArguments())
      .make()

    return proxyFactoryDynamicType
      .load(serviceInterface.classLoader, ClassLoadingStrategy.Default.INJECTION)
      .loaded
      .getConstructor()
      .newInstance() as Function<RoutingMetadata, Any>
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getRpcService0(serviceInterface: Class<T>, routing: RoutingMetadata): T {
    return proxyFactoryCache.get(serviceInterface).apply(routing) as T
  }
}
