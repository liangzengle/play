package play.example.robot.net

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.util.AttributeKey
import play.example.game.container.net.codec.RequestEncoder
import play.example.game.container.net.codec.ResponseDecoder
import play.example.robot.module.player.RobotPlayer
import play.mvc.Request
import play.net.netty.channelInitializer
import play.net.netty.createChannelFactory

class RobotClient(val host: String, val port: Int, val eventLoopGroup: EventLoopGroup) {

  companion object {
    val AttrKey = AttributeKey.valueOf<RobotClient>("RobotClient")
  }

  private var ch: Channel? = null

  private lateinit var player: RobotPlayer

  fun write(msg: Request) {
    val ch = this.ch
    if (ch != null && ch.isActive) {
      ch.writeAndFlush(msg, ch.voidPromise())
    }
  }

  fun connect() {
    val oldCh = ch
    ch = null
    oldCh?.close()

    val ch = Bootstrap()
      .group(eventLoopGroup)
      .remoteAddress(host, port)
      .channelFactory(createChannelFactory())
      .channelInitializer { ch ->
        ch.pipeline().addLast(RequestEncoder)
        ch.pipeline().addLast(ResponseDecoder(Int.MAX_VALUE))
        ch.pipeline().addLast(RobotChannelHandler)
      }
      .connect()
      .sync()
      .channel()
    ch.attr(AttrKey).set(this)
    this.ch = ch
  }

  fun getChannel(): Channel = ch!!

  fun setPlayer(player: RobotPlayer) {
    this.player = player
  }

  fun getPlayer(): RobotPlayer {
    return player
  }
}
