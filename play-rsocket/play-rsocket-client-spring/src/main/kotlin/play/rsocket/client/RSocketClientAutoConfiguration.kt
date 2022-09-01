package play.rsocket.client

import io.netty.buffer.ByteBuf
import io.rsocket.SocketAcceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import play.rsocket.client.event.MetadataPushApplicationEvent
import play.rsocket.event.RSocketMetadataPushEvent
import play.rsocket.rpc.*
import play.rsocket.security.SimpleTokenMetadata
import play.rsocket.serializer.ByteBufToIOStreamAdapter
import play.rsocket.serializer.RSocketCodec
import play.rsocket.serializer.RSocketSerializerProvider
import play.rsocket.transport.SmartTransportFactory
import play.rsocket.transport.TcpTransportFactory
import play.rsocket.transport.TransportFactory
import reactor.core.publisher.Sinks

/**
 *
 *
 * @author LiangZengle
 */
@AutoConfiguration
@EnableConfigurationProperties(RSocketClientProperties::class)
class RSocketClientAutoConfiguration {

  @Bean
  fun localServiceCallerRegistry(callers: List<LocalServiceCaller>): LocalServiceCallerRegistry {
    return LocalServiceCallerRegistryImpl(callers)
  }

  @Bean
  fun socketAcceptor(
    localServiceCallerRegistry: LocalServiceCallerRegistry,
    eventPublisher: ApplicationEventPublisher,
    codec: RSocketCodec
  ): BrokerClientSocketAcceptor {
    val sink = Sinks.many().unicast().onBackpressureError<ByteBuf>()
    sink.asFlux().doOnNext {
      val event = codec.decode(it, RSocketMetadataPushEvent::class.java) as RSocketMetadataPushEvent
      eventPublisher.publishEvent(MetadataPushApplicationEvent(event.data))
    }.subscribe()
    return BrokerClientSocketAcceptor { ClientRSocketResponder(localServiceCallerRegistry, codec::encode, sink) }
  }

  @ConditionalOnProperty(prefix = "rsocket.client.auth", value = ["type"], havingValue = "token")
  @Bean
  fun tokenMetadata(properties: RSocketClientProperties): RSocketClientCustomizer {
    val token = properties.auth.token
    val metadata = SimpleTokenMetadata(token)
    return RSocketClientCustomizer {
      it.setupMetadata(metadata.mimeType, metadata.content)
    }
  }

  @Bean
  fun tcpTransportFactory() = TcpTransportFactory()

  @Bean
  fun smartTransportFactory(transportFactories: List<TransportFactory>): SmartTransportFactory {
    return SmartTransportFactory(transportFactories)
  }

  @Bean
  fun brokerRSocketManager(
    properties: RSocketClientProperties,
    socketAcceptor: SocketAcceptor,
    clientCustomizers: ObjectProvider<RSocketClientCustomizer>,
    transportFactory: SmartTransportFactory,
    eventPublisher: ApplicationEventPublisher
  ): BrokerRSocketManager {
    return BrokerRSocketManagerImpl(
      properties.brokers.toSet(),
      properties.connectTimeout,
      socketAcceptor,
      clientCustomizers,
      transportFactory,
      eventPublisher
    )
  }

  @Bean
  @ConditionalOnMissingBean
  fun requester(brokerRSocketManager: BrokerRSocketManager, codec: RSocketCodec): AbstractRSocketRequester {
    return ClientRSocketRequester(brokerRSocketManager::getRSocket, codec)
  }

  @Bean
  @ConditionalOnMissingBean
  fun rpcClient(
    requester: AbstractRSocketRequester,
    ioStreamAdapter: ByteBufToIOStreamAdapter,
    serializerProvider: RSocketSerializerProvider
  ): RpcClient {
    return ProxyRpcClient(requester, ioStreamAdapter, serializerProvider)
  }
}
