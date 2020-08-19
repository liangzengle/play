package play.net.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelPromise
import io.netty.util.AttributeKey
import mu.KLogging

class NettyClient(
  private val name: String,
  bootstrap: Bootstrap,
  private val attributes: Map<AttributeKey<out Any>, Any> = emptyMap(),
  private val autoReconnect: Boolean = true
) {

  private val b = bootstrap.clone()

  private var ch: Channel? = null

  @Suppress("UNCHECKED_CAST")
  fun connect() {
    val ch = b.clone().connect().sync().channel()
    attributes.forEach { (k, v) -> ch.attr(k as AttributeKey<Any>).set(v) }
    this.ch = ch
  }

  @Synchronized
  private fun reconnect() {
    if (isConnected()) {
      return
    }
    connect()
  }

  fun disconnect() {
    ch?.close()?.sync()
  }

  fun write(msg: Any) {
    write(msg, true, null)
  }

  fun write(msg: Any, flush: Boolean, promise: ChannelPromise?) {
    if (!isConnected()) {
      if (!autoReconnect) {
        return
      }
      reconnect()
    }
    if (!isConnected()) {
      logger.error { "send msg failed: $msg" }
      return
    }
    val ch = this.ch
    if (flush) {
      ch?.writeAndFlush(msg, promise ?: ch.voidPromise())
    } else {
      ch?.write(msg, promise ?: ch.voidPromise())
    }
  }

  fun isConnected(): Boolean {
    return ch?.isActive ?: false
  }

  override fun toString(): String {
    return "NettyClient(name='$name', bootstrap=$b, attributes=$attributes, autoReconnect=$autoReconnect)"
  }

  companion object : KLogging()
}
