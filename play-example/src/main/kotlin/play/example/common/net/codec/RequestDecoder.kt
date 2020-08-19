package play.example.common.net.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import play.example.common.net.message.WireRequestBody
import play.example.request.RequestProto
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request

/**
 * ByteBuf -> Request
 * @author LiangZengle
 */
@ChannelHandler.Sharable
class RequestDecoder(private val maxFrameLength: Int) : ChannelInboundHandlerAdapter() {
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
    if (len > maxFrameLength) {
      msg.skipBytes(msg.readableBytes())
      // TODO throw exception
      return
    }
    val msgId = msg.readInt()
    val sequenceNo = msg.readInt()
    val payload = ByteArray(len - 8)
    msg.readBytes(payload)
    val body = RequestProto.ADAPTER.decode(payload)
    val request = Request(Header(MsgId(msgId), sequenceNo), WireRequestBody(body))
    ctx.fireChannelRead(request)
  }
}
