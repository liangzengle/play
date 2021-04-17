package play.example.game.base.net.codec

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import play.mvc.Response
import play.util.EmptyByteArray

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
    val sequenceNo = msg.header.sequenceNo
    val statusCode = msg.statusCode
    val body: Any? = msg.body
    val binaryBody = if (body == null) EmptyByteArray else PB.encode(body)
    val len = 12 + binaryBody.size
    val buffer = ctx.channel().alloc().ioBuffer(len)
    buffer
      .writeInt(len)
      .writeInt(msgId)
      .writeInt(sequenceNo)
      .writeInt(statusCode)
      .writeBytes(binaryBody)
    ctx.write(buffer, promise)
  }
}
