package play.rsocket.rpc

import com.alibaba.rsocket.cloudevents.CloudEventImpl
import com.alibaba.rsocket.listen.RSocketResponderHandlerFactory
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller
import com.alibaba.rsocket.upstream.UpstreamManager
import com.alibaba.spring.boot.rsocket.RSocketProperties
import io.rsocket.ConnectionSetupPayload
import io.rsocket.RSocket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.extra.processor.TopicProcessor

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("\${rsocket.disabled:false}==false")
class RSocketRpcConfiguration {
  @Bean
  @ConditionalOnMissingBean(type = ["brave.Tracing"])
  fun rsocketResponderHandlerFactory(
    @Autowired serviceCaller: RSocketServiceAnnotationProcessor,
    @Autowired @Qualifier("reactiveCloudEventProcessor") eventProcessor: TopicProcessor<CloudEventImpl<*>>
  ): RSocketResponderHandlerFactory {
    return RSocketResponderHandlerFactory { setupPayload: ConnectionSetupPayload, requester: RSocket ->
      Mono.fromCallable {
        RSocketResponderHandler(
          serviceCaller, eventProcessor, requester, setupPayload
        )
      }
    }
  }

  @Bean
  fun rsocketServiceAnnotationProcessor(properties: RSocketProperties): RSocketServiceAnnotationProcessor {
    return RSocketServiceAnnotationProcessor(properties)
  }

  @Bean
  fun localReactiveServiceCaller(processor: RSocketServiceAnnotationProcessor): LocalReactiveServiceCaller {
    return processor
  }

  @Bean
  fun rpcClient(upstreamManager: UpstreamManager): RpcClient {
    return RpcClient.create(upstreamManager)
  }

//  @Bean
//  fun rsocketRequester(upstreamManager: UpstreamManager): RSocketRequester? {
//    val loadBalancedRSocket = upstreamManager.findBroker().loadBalancedRSocket
//    val rSocketStrategies: RSocketStrategies = RSocketStrategies.builder()
//      .encoder(HessianEncoder())
//      .decoder(HessianDecoder())
//      .build()
//    return RSocketRequester.wrap(
//      loadBalancedRSocket,
//      MimeType.valueOf("application/x-hessian"),
//      MimeType.valueOf("message/x.rsocket.composite-metadata.v0"),
//      rSocketStrategies
//    )
//  }
}
