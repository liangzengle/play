package play.example.game.container.net.codec

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.ReferenceCountUtil
import play.mvc.Header
import play.mvc.MsgId
import play.mvc.Request
import play.mvc.RequestBodyFactory

/**
 * ByteBuf -> Request
 * @author LiangZengle
 */
class RequestDecoder(maxFrameLength: Int) :
  LengthFieldBasedFrameDecoder(maxFrameLength, 0, 4, 0, 4) {

  override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf?): Any? {
    val msg = super.decode(ctx, `in`)
    if (msg !is ByteBuf) {
      return msg
    }
    val request: Request
    try {
      val msgId = msg.readInt()
      val sequenceNo = msg.readInt()
      val requestBody = RequestBodyFactory.Default.parseFrom(ByteBufUtil.getBytes(msg))
      request = Request(Header(MsgId(msgId), sequenceNo), requestBody)
    } finally {
      ReferenceCountUtil.release(msg)
    }
    ctx.fireChannelRead(request)
    return null
  }
}
