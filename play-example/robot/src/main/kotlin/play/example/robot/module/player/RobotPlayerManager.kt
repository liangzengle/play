package play.example.robot.module.player

import com.typesafe.config.ConfigFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.robot.bt.DummyBehaviorTree
import play.example.robot.net.RobotClient
import play.net.netty.createEventLoopGroup

/**
 *
 * @author LiangZengle
 */
@Component
class RobotPlayerManager @Autowired constructor(private val bt: DummyBehaviorTree) : SmartInitializingSingleton {

  override fun afterSingletonsInstantiated() {
    val conf = ConfigFactory.load()
    val host = conf.getString("server.host")
    val port = conf.getInt("server.port")
    val num = conf.getInt("robot.num")

    val netEventLoop = createEventLoopGroup("robot-client", 4)
    val robotEventLoop = createEventLoopGroup("robot-executor", 8, false)
    for (i in 1..num) {
      val client = RobotClient(host, port, netEventLoop)
      val robotPlayer = RobotPlayer(i.toString(), client, robotEventLoop.next())
      client.connect()
      robotPlayer.execute {
        bt.run(robotPlayer)
      }
    }
  }
}
