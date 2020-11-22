package play.example.common.net.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import play.example.common.net.message.ByteBufInputStreamMessage
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Response

/**
 * ByteBuf -> Response
 * @author LiangZengle
 */
class ResponseDecoder : LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4) {
  override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any {
    val msg = super.decode(ctx, `in`)
    if (msg !is ByteBuf) {
      return msg
    }
    val msgId = msg.readInt()
    val sequenceNo = msg.readInt()
    val statusCode = msg.readInt()
    return Response(
      Header(MsgId(msgId), sequenceNo),
      statusCode,
      ByteBufInputStreamMessage(ByteBufInputStream(msg, msg.readableBytes(), true))
    )
  }
}
