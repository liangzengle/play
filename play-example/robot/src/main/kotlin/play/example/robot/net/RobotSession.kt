package play.example.robot.net

import io.netty.channel.Channel
import play.mvc.Request

class RobotSession {

  private lateinit var ch: Channel

  fun write(msg: Request) {
    ch.writeAndFlush(msg, ch.voidPromise())
  }
}
