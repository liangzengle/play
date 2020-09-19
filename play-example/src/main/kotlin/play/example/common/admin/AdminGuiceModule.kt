package play.example.common.admin

import com.google.inject.Provides
import io.netty.bootstrap.ServerBootstrap
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpRequestEncoder
import play.inject.guice.GuiceModule
import play.net.netty.TcpServer
import play.net.netty.channelInitializer
import play.net.netty.http.NettyHttpServerHandler
import javax.inject.Named
import javax.inject.Singleton

class AdminGuiceModule : GuiceModule() {
  override fun configure() {
    bind<NettyHttpServerHandler>().qualifiedWith("admin").to<AdminHttpServerHandler>()
    bind<AdminHttpController>().asEagerSingleton()
  }

  @Singleton
  @Provides
  @Named("admin-http")
  private fun adminHttp(
    serverBootstrap: ServerBootstrap,
    @Named("admin") handler: NettyHttpServerHandler
  ): TcpServer {
    val host = ctx.conf.getString("admin.host")
    val port = ctx.conf.getInt("admin.port")
    serverBootstrap.channelInitializer {
      it.pipeline().addLast(HttpRequestDecoder())
      it.pipeline().addLast(HttpRequestEncoder())
      it.pipeline().addLast(HttpObjectAggregator(2048, true))
      it.pipeline().addLast("handler", handler)
    }
    return TcpServer("admin-http", host, port, serverBootstrap)
  }
}
