package play.example.rpc.test

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import play.rsocket.client.RSocketClientAutoConfiguration
import play.rsocket.client.RSocketClientCustomizer
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType
import play.rsocket.rpc.RpcClient
import play.rsocket.rpc.RpcClientInterceptor

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Import(value = [RSocketClientAutoConfiguration::class])
class RpcTestApp {

  @Bean
  fun idAndRole(): RSocketClientCustomizer {
    return RSocketClientCustomizer { builder ->
      builder.id(100).role(2)
    }
  }

  @Bean
  fun config(): Config {
    return ConfigFactory.load()
  }

  @Bean
  fun interceptor(applicationContext: ApplicationContext): RpcClientInterceptor {
    val ctx = CacheableContext(applicationContext)
    return RpcClientInterceptor { rpcClient ->
      object : RpcClient {
        override fun <T : Any> getRpcService(serviceInterface: Class<T>, routing: RoutingMetadata): T {
          if (routing.routingType == RoutingType.UnicastToNode && routing.nodeId == 100) {
            val bean = ctx.getSingleton(serviceInterface)
            if (bean != null) {
              return bean
            }
          }
          return rpcClient.getRpcService(serviceInterface, routing)
        }
      }
    }
  }
}

fun main() {
  val context = SpringApplication.run(RpcTestApp::class.java)
  while (context.isActive) {
    Thread.sleep(10000)
  }
}
