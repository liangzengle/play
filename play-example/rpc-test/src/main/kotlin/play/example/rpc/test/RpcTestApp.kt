package play.example.rpc.test

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import play.rsocket.client.RSocketClientAutoConfiguration
import play.rsocket.client.RSocketClientCustomizer

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
}

fun main() {
  val context = SpringApplication.run(RpcTestApp::class.java)
  while (context.isActive) {
    Thread.sleep(10000)
  }
}
