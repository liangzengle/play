package play.example.game.base.net.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request
import play.mvc.RequestBody
import play.net.netty.copyToArray

/**
 * ByteBuf -> Request
 * @author LiangZengle
 */
class RequestDecoder(maxFrameLength: Int) : LengthFieldBasedFrameDecoder(maxFrameLength, 0, 4, 0, 4) {
  override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
    val msg = super.decode(ctx, `in`)
    if (msg !is ByteBuf) {
      return msg
    }
    val msgId = msg.readInt()
    val sequenceNo = msg.readInt()
    val requestBody = PB.decode<RequestBody>(msg.copyToArray())
    return Request(Header(MsgId(msgId), sequenceNo), requestBody)
  }
}
