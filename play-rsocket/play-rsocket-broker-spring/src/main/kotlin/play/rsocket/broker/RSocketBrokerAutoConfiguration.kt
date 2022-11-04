package play.rsocket.broker

import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.core.RSocketServer
import io.rsocket.core.Resume
import io.rsocket.loadbalance.LoadbalanceStrategy
import io.rsocket.metadata.WellKnownMimeType
import io.rsocket.transport.netty.server.TcpServerTransport
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.rsocket.server.RSocketServerCustomizer
import org.springframework.context.annotation.Bean
import play.rsocket.broker.acceptor.BrokerSocketAcceptor
import play.rsocket.broker.routing.*
import play.rsocket.broker.rsocket.RSocketFactory
import play.rsocket.broker.util.IntIdByteIndexMap
import play.rsocket.loadbalance.RandomLoadbalanceStrategy
import play.rsocket.metadata.MetadataExtractor
import play.rsocket.metadata.MimeTypes
import play.rsocket.metadata.RouteSetupMetadata
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.rpc.ImmutableLocalServiceCallerRegistry
import play.rsocket.rpc.LocalServiceCaller
import play.rsocket.rpc.LocalServiceCallerRegistry
import play.rsocket.security.SimpleTokenSocketAcceptorInterceptor
import play.rsocket.serializer.RSocketCodec
import reactor.netty.tcp.TcpServer

/**
 *
 *
 * @author LiangZengle
 */
@AutoConfiguration
@EnableConfigurationProperties(RSocketBrokerProperties::class)
class RSocketBrokerAutoConfiguration {

  @Bean
  fun rsocketBrokerServer(
    tcpServer: TcpServer, customizers: ObjectProvider<RSocketServerCustomizer>
  ): RSocketBrokerServer {
    val server = RSocketServer.create()
    customizers.forEach { it.customize(server) }
    val mono = server.bind(TcpServerTransport.create(tcpServer))
    return LifecycleRSocketBrokerServer(TcpRSocketBrokerServer(mono))
  }

  @Bean
  fun tcpServer(properties: RSocketBrokerProperties, customizers: ObjectProvider<TcpServerCustomizer>): TcpServer {
    val server = TcpServer.create().host(properties.host).port(properties.port)
    customizers.forEach { it.customize(server) }
    return server
  }

  @Bean
  fun rsocketResume(): RSocketServerCustomizer {
    return RSocketServerCustomizer { server ->
      server.resume(Resume())
    }
  }

  @ConditionalOnProperty(prefix = "rsocket.broker.auth", value = ["type"], havingValue = "token")
  @Bean
  fun simpleTokenRSocketSecurity(properties: RSocketBrokerProperties): RSocketServerCustomizer {
    val token = properties.auth.token
    return RSocketServerCustomizer { server ->
      server.interceptors { registry -> registry.forSocketAcceptor(SimpleTokenSocketAcceptorInterceptor(token)) }
    }
  }

  @Bean
  fun routingTable(): RoutingTable {
    return RoutingTable(IntIdByteIndexMap())
  }

  @Bean
  fun loadbalanceStrategy(): LoadbalanceStrategy {
    return RandomLoadbalanceStrategy()
  }

  @Bean
  fun rsocketQuery(routingTable: RoutingTable): RSocketQuery {
    return DefaultRSocketQuery(routingTable)
  }

  @Bean
  fun rsocketLocator(rsocketQuery: RSocketQuery, loadbalanceStrategy: LoadbalanceStrategy): RSocketLocator {
    val unicastRSocketLocator = UnicastRSocketLocator(rsocketQuery, loadbalanceStrategy)
    val multicastRSocketLocator = MulticastRSocketLocator(rsocketQuery)
    return CompositeRSocketLocator(listOf(unicastRSocketLocator, multicastRSocketLocator))
  }

  @Bean
  fun localServiceCallerRegistry(callers: List<LocalServiceCaller>): LocalServiceCallerRegistry {
    return ImmutableLocalServiceCallerRegistry(callers)
  }

  @Bean
  fun recievingRSocketFactory(
    properties: RSocketBrokerProperties,
    rsocketLocator: RSocketLocator,
    localServiceCallerRegistry: LocalServiceCallerRegistry,
    codec: RSocketCodec
  ): RSocketFactory {
    val routingMetadataExtractor: (Payload) -> RoutingMetadata? = {
      MetadataExtractor.extract(
        it,
        WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
        WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.string,
        RoutingMetadata.DefaultInstance::parseFrom
      )
    }

    return RSocketFactory {
      BrokerRSocketResponder(
        properties.id, rsocketLocator, routingMetadataExtractor, localServiceCallerRegistry, codec::encode
      )
    }
  }

  @Bean
  fun socketAcceptor(
    properties: RSocketBrokerProperties, routingTable: RoutingTable, receivingRSocketFactory: RSocketFactory
  ): RSocketServerCustomizer {
    val routingMetadataExtractor: (ConnectionSetupPayload) -> RouteSetupMetadata? =
      { MetadataExtractor.extract(it, MimeTypes.RouteSetup, RouteSetupMetadata.DefaultInstance::parseFrom) }
    val acceptor = BrokerSocketAcceptor(
      properties.id, routingTable, routingMetadataExtractor, receivingRSocketFactory
    )
    return RSocketServerCustomizer { server ->
      server.acceptor(acceptor)
    }
  }
}
