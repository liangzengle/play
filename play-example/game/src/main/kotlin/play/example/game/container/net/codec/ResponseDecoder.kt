package play.example.game.container.net.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.ReferenceCountUtil
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Response
import play.net.netty.copyToArray

/**
 * ByteBuf -> Response(Header, StatusCode, ByteBuf)
 * @author LiangZengle
 */
class ResponseDecoder(maxFrameLength: Int) : LengthFieldBasedFrameDecoder(maxFrameLength, 0, 4, 0, 4) {
  override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf?): Any? {
    val msg = super.decode(ctx, `in`)
    if (msg !is ByteBuf) {
      return msg
    }
    val response: Response
    try {
      val msgId = msg.readInt()
      val sequenceNo = msg.readInt()
      val statusCode = msg.readInt()
      response = Response(Header(MsgId(msgId), sequenceNo), statusCode, msg.copyToArray())
    } finally {
      ReferenceCountUtil.release(msg)
    }
    ctx.fireChannelRead(response)
    return null
  }
}
