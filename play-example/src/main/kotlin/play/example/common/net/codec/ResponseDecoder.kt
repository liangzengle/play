package play.example.common.net.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import play.mvc.ByteArrayMessage
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Response

/**
 * ByteBuf -> Request
 * @author LiangZengle
 */
@ChannelHandler.Sharable
object ResponseDecoder : ChannelInboundHandlerAdapter() {
  override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
    if (msg !is ByteBuf) {
      super.channelRead(ctx, msg)
      return
    }
    if (msg.readableBytes() < 4) {
      return
    }
    val len = msg.readInt()
    if (len < 0) {
      return
    }
    if (len > msg.readableBytes()) {
      msg.resetReaderIndex()
      return
    }
    val msgId = msg.readInt()
    val sequenceNo = msg.readInt()
    val statusCode = msg.readInt()
    val payload = ByteArray(len - 12)
    msg.readBytes(payload)
    val response = Response(Header(MsgId(msgId), sequenceNo), statusCode, ByteArrayMessage(payload))
    ctx.fireChannelRead(response)
  }
}
