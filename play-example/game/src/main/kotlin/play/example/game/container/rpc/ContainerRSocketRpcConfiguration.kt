package play.example.game.container.rpc

import com.alibaba.rsocket.cloudevents.CloudEventImpl
import com.alibaba.rsocket.listen.RSocketResponderHandlerFactory
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller
import com.alibaba.rsocket.upstream.UpstreamManager
import com.alibaba.spring.boot.rsocket.RSocketProperties
import com.alibaba.spring.boot.rsocket.hessian.HessianDecoder
import com.alibaba.spring.boot.rsocket.hessian.HessianEncoder
import io.rsocket.ConnectionSetupPayload
import io.rsocket.RSocket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeType
import play.example.common.rpc.RpcClient
import reactor.core.publisher.Mono
import reactor.extra.processor.TopicProcessor

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("\${rsocket.disabled:false}==false")
class ContainerRSocketRpcConfiguration {
  @Bean
  @ConditionalOnMissingBean(type = ["brave.Tracing"])
  fun rsocketResponderHandlerFactory(
    @Autowired serviceCaller: ContainerRSocketServiceAnnotationProcessor,
    @Autowired @Qualifier("reactiveCloudEventProcessor") eventProcessor: TopicProcessor<CloudEventImpl<*>>
  ): RSocketResponderHandlerFactory {
    return RSocketResponderHandlerFactory { setupPayload: ConnectionSetupPayload, requester: RSocket ->
      Mono.fromCallable {
        ContainerRSocketResponderHandler(
          serviceCaller, eventProcessor, requester, setupPayload
        )
      }
    }
  }

  @Bean
  fun containerRSocketServiceAnnotationProcessor(properties: RSocketProperties): ContainerRSocketServiceAnnotationProcessor {
    return ContainerRSocketServiceAnnotationProcessor(properties)
  }

  @Bean
  fun localReactiveServiceCaller(o: ContainerRSocketServiceAnnotationProcessor): LocalReactiveServiceCaller {
    return o
  }

  @Bean
  fun rpcClient(upstreamManager: UpstreamManager): RpcClient {
    return RpcClient(upstreamManager)
  }

  @Bean
  fun rsocketRequester(upstreamManager: UpstreamManager): RSocketRequester? {
    val loadBalancedRSocket = upstreamManager.findBroker().loadBalancedRSocket
    val rSocketStrategies: RSocketStrategies = RSocketStrategies.builder()
      .encoder(HessianEncoder())
      .decoder(HessianDecoder())
      .build()
    return RSocketRequester.wrap(
      loadBalancedRSocket,
      MimeType.valueOf("application/x-hessian"),
      MimeType.valueOf("message/x.rsocket.composite-metadata.v0"),
      rSocketStrategies
    )
  }
}
