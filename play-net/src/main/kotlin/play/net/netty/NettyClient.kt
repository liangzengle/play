package play.net.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import play.util.logging.WithLogger

class NettyClient(
  private val name: String,
  bootstrap: Bootstrap,
  private val attributes: Map<AttributeKey<out Any>, Any> = emptyMap(),
  private val autoReconnect: Boolean = true
) : AutoCloseable {

  companion object : WithLogger()

  private val b = bootstrap.clone()

  private var ch: Channel? = null

  @Volatile
  private var closed = false

  private val connectPromise = PlayPromise.make<Unit>()

  override fun close() {
    disconnect()
  }

  @Suppress("UNCHECKED_CAST")
  fun connect(): PlayFuture<Unit> {
    b.connect().toPlay().onSuccess { ch ->
      attributes.forEach { (k, v) -> ch.attr(k as AttributeKey<Any>).set(v) }
      this.ch = ch
      connectPromise.success(Unit)
      if (autoReconnect) {
        ch.closeFuture().addListener {
          if (!closed) {
            connect()
          }
        }
      }
    }
    return connectPromise.future
  }

  fun disconnect() {
    closed = true
    ch?.close()
  }

  fun write(msg: Any) {
    write(msg, true, null)
  }

  fun write(msg: Any, flush: Boolean, promise: ChannelPromise?) {
    val ch = this.ch
    if (ch == null || !ch.isActive || !ch.isWritable) {
      logger.error { "send msg failed: $msg" }
      return
    }
    if (flush) {
      ch.writeAndFlush(msg, promise ?: ch.voidPromise())
    } else {
      ch.write(msg, promise ?: ch.voidPromise())
    }
  }

  fun isConnected(): Boolean {
    return ch?.isActive ?: false
  }

  override fun toString(): String {
    return "NettyClient(name='$name', bootstrap=$b, attributes=$attributes, autoReconnect=$autoReconnect)"
  }

}
