package play.example.game.container.net.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Response
import play.net.netty.copyToArray

/**
 * ByteBuf -> Response(Header, StatusCode, ByteBuf)
 * @author LiangZengle
 */
class ResponseDecoder : LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4) {
  override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
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
      msg.copyToArray()
    )
  }
}
