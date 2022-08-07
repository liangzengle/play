package play.rsocket.broker

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


/**
 *
 *
 * @author LiangZengle
 */
@SpringBootApplication
class BrokerApp

fun main(args: Array<String>) {
  val ctx = SpringApplication.run(BrokerApp::class.java, *args)
  while (ctx.isRunning) {
    Thread.sleep(10000)
  }
}
