package play.example.game.app.rpc

import com.alibaba.rsocket.RSocketRequesterSupport
import com.alibaba.spring.boot.rsocket.EnvironmentProperties
import com.alibaba.spring.boot.rsocket.RSocketProperties
import com.alibaba.spring.boot.rsocket.responder.RSocketServicesPublishHook
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportBuilderImpl
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportCustomizer
import io.rsocket.SocketAcceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import play.example.game.container.gs.domain.GameServerId
import play.rsocket.rpc.RSocketServiceAnnotationProcessor
import play.util.json.Json

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("\${rsocket.disabled:false}==false")
class GameRSocketRpcConfiguration {
  @Bean
  fun rsocketServicesPublishHook(): RSocketServicesPublishHook {
    return RSocketServicesPublishHook()
  }

  @Bean
  fun rsocketRequesterSupport(
    properties: RSocketProperties,
    environment: Environment,
    socketAcceptor: SocketAcceptor,
    customizers: ObjectProvider<RSocketRequesterSupportCustomizer>,
    gameServerId: GameServerId
  ): RSocketRequesterSupport {
    val propertiesCopy = Json.to<RSocketProperties>(Json.stringify(properties))
    propertiesCopy.group = gameServerId.toInt().toString()
    val builder = RSocketRequesterSupportBuilderImpl(propertiesCopy, EnvironmentProperties(environment), socketAcceptor)
    customizers.orderedStream().forEach { it.customize(builder) }
    return builder.build()
  }

  @Bean
  fun gameRSocketServiceAnnotationProcessor(
    rSocketServiceAnnotationProcessor: RSocketServiceAnnotationProcessor, gameServerId: GameServerId
  ): GameRSocketServiceAnnotationProcessor {
    return GameRSocketServiceAnnotationProcessor(rSocketServiceAnnotationProcessor, gameServerId)
  }
}
