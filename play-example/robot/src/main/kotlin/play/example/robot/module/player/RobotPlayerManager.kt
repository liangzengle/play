package play.example.robot.module.player

import com.typesafe.config.ConfigFactory
import io.netty.bootstrap.Bootstrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.container.net.codec.RequestEncoder
import play.example.game.container.net.codec.ResponseDecoder
import play.example.robot.bt.DummyBehaviorTree
import play.example.robot.net.RobotChannelHandler
import play.example.robot.net.RobotClient
import play.net.netty.NettyClient
import play.net.netty.channelInitializer
import play.net.netty.createChannelFactory
import play.net.netty.createEventLoopGroup
import kotlin.concurrent.thread

/**
 *
 * @author LiangZengle
 */
@Component
class RobotPlayerManager @Autowired constructor(private val bt: DummyBehaviorTree) {

  fun init() {
    val conf = ConfigFactory.load()
    val host = conf.getString("server.host")
    val port = conf.getInt("server.port")
    val num = conf.getInt("robot.num")
    val serverIds = conf.getIntList("server.ids")

    val netEventLoop = createEventLoopGroup("robot-client", 8)
    val robotEventLoop = createEventLoopGroup("robot-executor", 8, false)
    val b = Bootstrap()
      .group(netEventLoop)
      .remoteAddress(host, port)
      .channelFactory(createChannelFactory())
      .channelInitializer { ch ->
        ch.pipeline().addLast(RequestEncoder)
        ch.pipeline().addLast(ResponseDecoder(Int.MAX_VALUE))
        ch.pipeline().addLast(RobotChannelHandler)
      }

    for (i in 1..num) {
      val robotPlayer = RobotPlayer(i.toString(), robotEventLoop.next())
      robotPlayer.serverId = serverIds[i % serverIds.size]
      val nettyClient = NettyClient("robot-$i", b, mapOf(RobotPlayer.AttrKey to robotPlayer))
      val client = RobotClient(nettyClient)
      robotPlayer.setClient(client)
      client.connect().onSuccess {
        robotPlayer.execute {
          bt.run(robotPlayer)
        }
      }
    }

    Runtime.getRuntime().addShutdownHook(thread(false) {
      netEventLoop.shutdownGracefully().sync()
      robotEventLoop.shutdownGracefully().sync()
    })
  }
}
