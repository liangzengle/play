package play.example.robot.net

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import play.mvc.Response

/**
 *
 * @author LiangZengle
 */
@ChannelHandler.Sharable
object RobotChannelHandler : SimpleChannelInboundHandler<Response>() {
  private lateinit var dispatcher: ResponseDispatcher

  override fun channelRead0(ctx: ChannelHandlerContext, msg: Response) {
    dispatcher.dispatch(ctx, msg)
  }

  fun setDispatcher(dispatcher: ResponseDispatcher) {
    this.dispatcher = dispatcher
  }
}
