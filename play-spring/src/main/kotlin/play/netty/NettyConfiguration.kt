package play.netty

import io.netty.channel.EventLoopGroup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.GracefullyShutdown
import play.net.netty.createEventLoopGroup
import play.net.netty.toPlay

/**
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class NettyConfiguration {

  companion object {
    const val NETTY_SELECTOR = "netty-selector"
    const val NETTY_IO = "netty-io"
  }

  @Bean(NETTY_SELECTOR)
  fun nettySelector(shutdown: GracefullyShutdown): EventLoopGroup {
    val executor = createEventLoopGroup(NETTY_SELECTOR, 1)
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_ACCEPTOR, GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_ACCEPTOR, executor
    ) {
      it.shutdownGracefully().toPlay()
    }
    return executor
  }

  @Bean(NETTY_IO, autowireCandidate = true)
  fun nettyIO(shutdown: GracefullyShutdown): EventLoopGroup {
    val executor = createEventLoopGroup(NETTY_IO, 0)
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_WORKER, GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_WORKER, executor
    ) {
      it.shutdownGracefully().toPlay()
    }
    return executor
  }
}
