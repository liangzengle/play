package play.example.common.net.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import play.example.common.net.message.WireRequestBody
import play.example.request.RequestProto
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request

/**
 * ByteBuf -> Request
 * @author LiangZengle
 */
class RequestDecoder(maxFrameLength: Int) : LengthFieldBasedFrameDecoder(maxFrameLength, 0, 4, 0, 4) {
  override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any {
    val msg = super.decode(ctx, `in`)
    if (msg !is ByteBuf) {
      return msg
    }
    val msgId = msg.readInt()
    val sequenceNo = msg.readInt()
    val body = RequestProto.ADAPTER.decode(ByteBufInputStream(msg, msg.readableBytes(), true))
    return Request(Header(MsgId(msgId), sequenceNo), WireRequestBody(body))
  }
}
