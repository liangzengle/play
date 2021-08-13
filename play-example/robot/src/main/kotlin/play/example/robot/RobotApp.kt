package play.example.robot

import com.typesafe.config.ConfigFactory

object RobotApp {

  @JvmStatic
  fun main(args: Array<String>) {
    val conf = ConfigFactory.load()
    val host = conf.getString("server.host")
    val port = conf.getString("server.port")
    val num = conf.getInt("robot.num")
    for (i in 1..num) {
      
    }
  }
}
