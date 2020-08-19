package play.net.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel

class TcpClient(
  private val name: String,
  private val host: String,
  private val port: Int,
  bootstrap: Bootstrap,
  private val autoReconnect: Boolean = true
) {

  private val b = bootstrap.clone()

  private var ch: Channel? = null

  fun connect() {
    ch = b.clone().connect(host, port).sync().channel()
  }

  fun disconnect() {
    ch?.close()?.sync()
  }

  fun write(msg: Any) {
    if (!isConnected()) {
      if (!autoReconnect) {
        return
      }
      connect()
    }
    ch?.writeAndFlush(msg)
  }

  fun isConnected(): Boolean {
    return ch?.isActive ?: false
  }

  override fun toString(): String {
    return "TcpClient(name='$name', host='$host', port=$port, autoReconnect=$autoReconnect)"
  }
}
