package play.example.rpc.test

import com.alibaba.rsocket.upstream.UpstreamManager
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.common.rpc.RpcClient

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
class RpcTestApp {

  @Bean
  fun rpcClient(upstreamManager: UpstreamManager): RpcClient {
    return RpcClient(upstreamManager)
  }
}

fun main() {
  SpringApplication.run(RpcTestApp::class.java)
}
