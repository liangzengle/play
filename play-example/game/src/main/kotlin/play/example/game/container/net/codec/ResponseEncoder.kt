package play.example.game.container.net.codec

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import play.mvc.Response

/**
 * Response -> ByteBuf
 * @author LiangZengle
 */
@ChannelHandler.Sharable
object ResponseEncoder : ChannelOutboundHandlerAdapter() {
  override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise?) {
    if (msg !is Response) {
      super.write(ctx, msg, promise)
      return
    }
    val msgId = msg.header.msgId.value
    val requestId = msg.header.requestId
    val statusCode = msg.statusCode
    val len = 12 + msg.body.size
    val buffer = ctx.channel().alloc().ioBuffer(len)
    buffer
      .writeInt(len)
      .writeInt(msgId)
      .writeInt(requestId)
      .writeInt(statusCode)
      .writeBytes(msg.body)
    ctx.write(buffer, promise)
  }
}
