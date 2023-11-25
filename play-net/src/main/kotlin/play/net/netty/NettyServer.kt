package play.net.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import play.net.NetServer
import play.util.concurrent.PlayFuture
import play.util.control.getCause
import play.util.logging.WithLogger

class NettyServer(
  val name: String, val host: String, val port: Int, bootstrap: ServerBootstrap
) : NetServer {
  companion object : WithLogger()

  private val b = bootstrap.clone()

  private var ch: Channel? = null

  override fun startAsync(): PlayFuture<*> {
    val future = b.bind(host, port).toPlay()
    future.onComplete {
      if (it.isSuccess) {
        this.ch = it.getOrThrow()
        logger.info { "NettyServer [$name] started, listening on $host:$port" }
      } else {
        logger.error(it.getCause()) { "NettyServer [$name] failed to bind $host:$port" }
      }
    }
    return future
  }

  override fun stopAsync(): PlayFuture<*> {
    val c = ch ?: return PlayFuture.successful(null)
    val future = c.close().toPlay()
    future.onComplete {
      if (it.isSuccess) {
        logger.info { "NettyServer [$name] stopped" }
      } else {
        logger.error(it.getCause()) { "NettyServer [$name] failed to stop" }
      }
    }
    return future
  }

  override fun toString(): String {
    return "NettyServer(name='$name', host='$host', port=$port)"
  }
}
