package play.example.game.container.net.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import play.mvc.Request
import play.util.exception.NoStackTraceException

/**
 *
 * @author LiangZengle
 */
class RequestIdValidator : ChannelInboundHandlerAdapter() {

  private var nextRequestId = 1

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg !is Request) {
      ctx.fireChannelRead(msg)
      return
    }
    val requestId = msg.header.requestId
    if (requestId < nextRequestId) {
      val errorMsg = "expected:$nextRequestId, received:$requestId, channel:${ctx.channel()}"
      ctx.fireExceptionCaught(InvalidRequestIdException(errorMsg))
      return
    }
    nextRequestId = requestId + 1
    if (nextRequestId < 0) {
      nextRequestId = 1
    }
    ctx.fireChannelRead(msg)
  }
}

class InvalidRequestIdException(msg: String) : NoStackTraceException(msg)
