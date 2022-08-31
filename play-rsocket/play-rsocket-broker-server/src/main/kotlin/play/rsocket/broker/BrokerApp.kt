package play.rsocket.broker

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 *
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
class BrokerApp {

  @Bean
  fun config(): Config {
    return ConfigFactory.load()
  }
}

fun main(args: Array<String>) {
  val ctx = SpringApplication.run(BrokerApp::class.java, *args)
  while (ctx.isRunning) {
    Thread.sleep(10000)
  }
}
