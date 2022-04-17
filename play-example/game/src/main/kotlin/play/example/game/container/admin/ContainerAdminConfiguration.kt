package play.example.game.container.admin

import com.typesafe.config.Config
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder
import play.net.netty.http.NettyHttpServerHandler
import play.util.primitive.toIntSaturated

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class ContainerAdminConfiguration {

  @Bean("adminHttpServer")
  fun adminHttpServer(
    conf: Config,
    serverBuilder: NettyServerBuilder,
    handler: NettyHttpServerHandler
  ): NettyServer {
    val host = conf.getString("play.admin.host")
    val port = conf.getInt("play.admin.port")
    val maxContentLength = conf.getMemorySize("play.admin.max-content-length").toBytes().toIntSaturated()
    return serverBuilder
      .host(host)
      .port(port)
      .childHandler { ch ->
        ch.pipeline().apply {
          addLast(HttpServerCodec())
          addLast(HttpObjectAggregator(maxContentLength, true))
          addLast("handler", handler)
        }
      }
      .build("admin-http")
  }

  @Bean
  fun httpActionManager(): ContainerAdminHttpActionManager {
    return ContainerAdminHttpActionManager()
  }

  @Bean
  fun httpServerHandler(actionManager: ContainerAdminHttpActionManager): NettyHttpServerHandler {
    return ContainerAdminHttpServerHandler(actionManager)
  }
}
