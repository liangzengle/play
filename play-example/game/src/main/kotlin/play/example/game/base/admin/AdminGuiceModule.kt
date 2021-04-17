package play.example.game.base.admin

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Provides
import com.typesafe.config.Config
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import javax.inject.Named
import javax.inject.Singleton
import play.inject.guice.GuiceModule
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder
import play.net.netty.http.NettyHttpServerHandler

@AutoService(Module::class)
class AdminGuiceModule : GuiceModule() {
  override fun configure() {
    bindSingleton<AdminHttpServerHandler>()
    bindNamed<NettyHttpServerHandler>("admin").to(binding<AdminHttpServerHandler>())
    bind<AdminHttpController>().asEagerSingleton()
  }

  @Singleton
  @Provides
  @Named("admin-http")
  private fun adminHttp(
    conf: Config,
    serverBuilder: NettyServerBuilder,
    @Named("admin") handler: NettyHttpServerHandler
  ): NettyServer {
    val host = conf.getString("admin.host")
    val port = conf.getInt("admin.port")
    return serverBuilder
      .host(host)
      .port(port)
      .childHandler { ch ->
        ch.pipeline().addLast(HttpServerCodec())
        ch.pipeline().addLast(HttpObjectAggregator(1024, true))
        ch.pipeline().addLast("handler", handler)
      }
      .build("admin-http")
  }
}
