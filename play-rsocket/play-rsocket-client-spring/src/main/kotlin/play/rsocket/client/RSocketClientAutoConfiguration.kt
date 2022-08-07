package play.rsocket.client

import io.netty.buffer.ByteBuf
import io.rsocket.SocketAcceptor
import io.rsocket.core.Resume
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
import play.rsocket.serializer.PlaySerializerProvider
import play.rsocket.serializer.kryo.KryoCodec
import play.rsocket.serializer.kryo.KryoSerializerProvider
import play.rsocket.serializer.kryo.io.ByteBufToInputOutput
import play.rsocket.transport.SmartTransportFactory
import play.rsocket.transport.TcpTransportFactory
import play.rsocket.transport.TransportFactory
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.time.Duration

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
    eventPublisher: ApplicationEventPublisher
  ): BrokerClientSocketAcceptor {
    val sink = Sinks.many().unicast().onBackpressureError<ByteBuf>()
    sink.asFlux()
      .doOnNext {
        val event = KryoCodec.decoder(it, RSocketMetadataPushEvent::class.java) as RSocketMetadataPushEvent
        eventPublisher.publishEvent(MetadataPushApplicationEvent(event.data))
      }.subscribe()
    return BrokerClientSocketAcceptor { ClientRSocketResponder(localServiceCallerRegistry, KryoCodec.encoder, sink) }
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
      socketAcceptor,
      clientCustomizers,
      transportFactory,
      eventPublisher
    )
  }

  @Bean
  fun rsocketResume(): RSocketClientCustomizer {
    return RSocketClientCustomizer { builder ->
      builder.customizeConnector { connector ->
        connector.resume(Resume())
      }
    }
  }

  @Bean
  fun reconnect(): RSocketClientCustomizer {
    return RSocketClientCustomizer { builder ->
      builder.customizeConnector { connector ->
        connector.reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10)))
      }
    }
  }

  @Bean
  @ConditionalOnMissingBean
  fun requester(brokerRSocketManager: BrokerRSocketManager): AbstractRSocketRequester {
    return ClientRSocketRequester(brokerRSocketManager::getRSocket, KryoCodec.decoder, KryoCodec.encoder)
  }

  @Bean
  @ConditionalOnMissingBean
  fun ioStreamAdapter(): ByteBufToIOStreamAdapter {
    return ByteBufToInputOutput
  }

  @Bean
  @ConditionalOnMissingBean
  fun serializerProvider(): PlaySerializerProvider {
    return KryoSerializerProvider
  }

  @Bean
  @ConditionalOnMissingBean
  fun rpcClient(
    requester: AbstractRSocketRequester,
    ioStreamAdapter: ByteBufToIOStreamAdapter,
    serializerProvider: PlaySerializerProvider
  ): RpcClient {
    return ProxyRpcClient(requester, ioStreamAdapter, serializerProvider)
  }
}
