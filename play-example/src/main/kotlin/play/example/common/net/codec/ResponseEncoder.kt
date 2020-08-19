package play.example.common.net.codec

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import play.mvc.Response

/**
 * WireMessage -> ByteBuf
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
    val sequenceNo = msg.header.sequenceNo
    val statusCode = msg.statusCode
    val body = msg.body.toByteArray()
    val len = 12 + body.size
    val buffer = ctx.channel().alloc().ioBuffer(len)
    buffer
      .writeInt(len)
      .writeInt(msgId)
      .writeInt(sequenceNo)
      .writeInt(statusCode)
      .writeBytes(body)
    ctx.write(buffer, promise)
  }
}
