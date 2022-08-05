package play.example.game.container.net.codec

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import play.codec.MessageCodec
import play.mvc.Request

/**
 * Request -> ByteBuf
 * @author LiangZengle
 */
@ChannelHandler.Sharable
object RequestEncoder : ChannelOutboundHandlerAdapter() {

  override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise?) {
    if (msg !is Request) {
      super.write(ctx, msg, promise)
      return
    }

    val msgId = msg.header.msgId.value
    val sequenceNo = msg.header.requestId
    val body = MessageCodec.encode(msg.body)
    val len = 8 + body.size
    val buffer = ctx.channel().alloc().ioBuffer(len)
    buffer
      .writeInt(len)
      .writeInt(msgId)
      .writeInt(sequenceNo)
      .writeBytes(body)
    ctx.write(buffer, promise)
  }
}
