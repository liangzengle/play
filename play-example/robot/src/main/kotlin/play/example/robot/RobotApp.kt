package play.example.robot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import play.example.robot.net.ResponseDispatcher
import play.example.robot.net.RobotChannelHandler
import play.spring.getInstance

@SpringBootApplication
class RobotAppSource

object RobotApp {

  @JvmStatic
  fun main(args: Array<String>) {
    val applicationContext = SpringApplication.run(RobotAppSource::class.java)
    val responseDispatcher = applicationContext.getInstance<ResponseDispatcher>()
    RobotChannelHandler.setDispatcher(responseDispatcher)
  }
}
