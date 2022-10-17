package play.example.robot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Configuration
import play.example.common.App
import play.example.robot.module.player.RobotPlayerManager
import play.example.robot.net.ResponseDispatcher
import play.example.robot.net.RobotChannelHandler
import play.spring.getInstance

@SpringBootApplication
@Configuration(proxyBeanMethods = false)
class RobotAppSource

object RobotApp : App() {

  @JvmStatic
  fun main(args: Array<String>) {
    val applicationContext = SpringApplication.run(RobotAppSource::class.java)
    val responseDispatcher = applicationContext.getInstance<ResponseDispatcher>()
    RobotChannelHandler.setDispatcher(responseDispatcher)

    val playerManager = applicationContext.getInstance<RobotPlayerManager>()
    playerManager.init()
  }
}
