package play.example.game.base.net

import akka.actor.typed.ActorRef
import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.typesafe.config.Config
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.WriteBufferWaterMark
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import play.example.game.base.net.codec.RequestDecoder
import play.example.game.base.net.codec.ResponseEncoder
import play.inject.guice.GuiceModule
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder
import play.net.netty.createEventLoopGroup

@AutoService(Module::class)
class NetGuiceModule : GuiceModule() {
  override fun configure() {
    bind(binding<EventLoopGroup>("net-accept"))
      .toProvider(EventLoopProvider("net-accept", 1))
    bind(binding<EventLoopGroup>("net-io"))
      .toProvider(EventLoopProvider("net-io", 0))
  }

  @Provides
  @Singleton
  @Named("net")
  fun config(config: Config): Config {
    return config.getConfig("net")
  }

  @Singleton
  @Provides
  @Named("game")
  private fun webSocketServer(
    @Named("net") conf: Config,
    serverBuilder: NettyServerBuilder,
    sessionManager: ActorRef<SessionManager.Command>
  ): NettyServer {
    val host = conf.getString("host")
    val port = conf.getInt("port")
    val encoder = ResponseEncoder
    return serverBuilder
      .host(host)
      .port(port)
      .childHandler { ch ->
        ch.pipeline().addLast(encoder)
        ch.pipeline().addLast(RequestDecoder(1024))

        ch.config().isAutoRead = false
        sessionManager.tell(SessionManager.CreateSession(ch))
      }
      .build("game")
  }

  @Provides
  private fun nettyServerBuilder(
    @Named("net-accept") parent: EventLoopGroup,
    @Named("net-io") child: EventLoopGroup,
  ): NettyServerBuilder {
    return NettyServerBuilder()
      .eventLoopGroup(parent, child)
      .option(ChannelOption.SO_REUSEADDR, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
      .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
      .childOption(ChannelOption.SO_RCVBUF, 8 * 1024)
      .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
  }
}

private class EventLoopProvider(val threadNamePrefix: String, val nThread: Int) : Provider<EventLoopGroup> {
  private val eventLoopGroup by lazy {
    createEventLoopGroup(threadNamePrefix, nThread)
  }

  override fun get(): EventLoopGroup = eventLoopGroup
}
