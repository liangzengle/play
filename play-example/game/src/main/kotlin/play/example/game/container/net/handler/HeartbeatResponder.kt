package play.example.game.container.net.handler

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import play.example.game.container.net.NetModule
import play.mvc.Request
import play.mvc.Response
import play.util.time.Time

/**
 *
 *
 * @author LiangZengle
 */
@Sharable
object HeartbeatResponder : ChannelInboundHandlerAdapter() {

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg is Request && msg.msgId() == NetModule.heartbeat) {
      ctx.writeAndFlush(Response(msg.header, 0, Time.currentMillis()))
    } else {
      ctx.fireChannelRead(msg)
    }
  }

}
