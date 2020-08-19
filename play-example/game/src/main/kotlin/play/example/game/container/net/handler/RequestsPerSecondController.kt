package play.example.game.container.net.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import play.mvc.Request
import play.util.exception.NoStackTraceException
import play.util.time.Time

/**
 * 请求数量控制
 *
 * @property permitsPerSecond 每秒请求数量上限
 */
open class RequestsPerSecondController(private val permitsPerSecond: Int) : ChannelInboundHandlerAdapter() {
  init {
    require(permitsPerSecond > 0) { "`permitsPerSecond` should be positive int." }
  }

  private var lastResetTime = 0L
  private val history = IntArrayList(permitsPerSecond)
  private var reject = false

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg !is Request) {
      ctx.fireChannelRead(msg)
      return
    }
    if (reject) {
      ReferenceCountUtil.release(msg)
      return
    }
    if (history.size() >= permitsPerSecond) {
      val now = Time.currentMillis()
      val elapsed = now - lastResetTime
      if (elapsed < 1000) {
        val errorMsg =
          "${history.size()} requests received in ${elapsed}ms from ${ctx.channel()}, they are: $history"
        ctx.fireExceptionCaught(TooManyRequestsException(errorMsg))
        reject = true
      }
      lastResetTime = now
      history.clear()
    } else {
      history.add(msg.msgId())
    }
    if (!reject) {
      ctx.fireChannelRead(msg)
    }
  }
}

class TooManyRequestsException(msg: String) : NoStackTraceException(msg)
