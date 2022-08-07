package play.example.game.container.net.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.ReadTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 *
 * @author LiangZengle
 */
class FireEventOnReadTimeoutHandler(timeout: Long, unit: TimeUnit) : ReadTimeoutHandler(timeout, unit) {
  constructor(timeout: Duration) : this(timeout.toMillis(), TimeUnit.MILLISECONDS)

  override fun readTimedOut(ctx: ChannelHandlerContext) {
    ctx.fireUserEventTriggered(IdleState.READER_IDLE)
  }
}
