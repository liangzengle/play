package play.net.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import play.net.NetServer
import play.util.logging.getLogger

class NettyServer(
  val name: String,
  val host: String,
  val port: Int,
  bootstrap: ServerBootstrap
) : NetServer {
  companion object {
    @JvmStatic
    private val logger = getLogger()
  }

  private val b = bootstrap.clone()

  private var ch: Channel? = null

  override fun start() {
    ch = b.bind(host, port).sync().channel()
    logger.info { "$name server started, listening on $host:$port" }
  }

  override fun stop() {
    var ex: InterruptedException? = null
    try {
      b.config().group()?.shutdownGracefully()?.sync()
    } catch (e: InterruptedException) {
      ex = e
    }
    try {
      b.config().childGroup()?.shutdownGracefully()?.sync()
    } catch (e: InterruptedException) {
      ex = e
    }
    if (ex != null) throw ex
    logger.info { "$name server stopped." }
  }

  override fun toString(): String {
    return "NettyServer(name='$name', host='$host', port=$port)"
  }
}
