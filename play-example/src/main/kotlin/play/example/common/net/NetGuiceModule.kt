package play.example.common.net

import akka.actor.typed.ActorRef
import com.google.inject.Provides
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import play.Configuration
import play.example.common.net.codec.RequestDecoder
import play.example.common.net.codec.ResponseEncoder
import play.inject.guice.GuiceModule
import play.net.netty.TcpServer
import play.net.netty.channelInitializer
import play.net.netty.createEventLoopGroup
import play.net.netty.createServerChannelFactory
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

class NetGuiceModule : GuiceModule() {
  override fun configure() {
    bind<Configuration>().qualifiedWith("net").toInstance(ctx.conf.getConfiguration("net"))
    bind<EventLoopGroup>().qualifiedWith("acceptor").toProvider(EventLoopProvider("acceptor", 1))
    bind<EventLoopGroup>().qualifiedWith("io").toProvider(EventLoopProvider("io", 0))
  }

  @Singleton
  @Provides
  @Named("game")
  private fun webSocketServer(
    @Named("net") conf: Configuration,
    serverBootstrap: ServerBootstrap,
    sessionManager: ActorRef<SessionManager.Command>
  ): TcpServer {
    val host = conf.getString("host")
    val port = conf.getInt("port")
    val encoder = ResponseEncoder
    val decoder = RequestDecoder(1024)
    serverBootstrap.channelInitializer {
      it.pipeline().addLast(encoder)
      it.pipeline().addLast(decoder)

      it.config().isAutoRead = false
      sessionManager.tell(SessionManager.CreateSession(it))
    }
    return TcpServer("game", host, port, serverBootstrap)
  }

  @Provides
  private fun serverBootstrap(
    @Named("acceptor") parent: EventLoopGroup,
    @Named("io") child: EventLoopGroup,
  ): ServerBootstrap {
    return ServerBootstrap()
      .group(parent, child)
      .channelFactory(createServerChannelFactory())
      .option(ChannelOption.SO_REUSEADDR, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
  }
}

private class EventLoopProvider(val threadNamePrefix: String, val nThread: Int) : Provider<EventLoopGroup> {
  private val eventLoopGroup: Lazy<EventLoopGroup> = lazy {
    createEventLoopGroup(threadNamePrefix, nThread)
  }

  override fun get(): EventLoopGroup = eventLoopGroup.value
}
