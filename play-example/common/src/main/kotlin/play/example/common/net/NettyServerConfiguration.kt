package play.example.common.net

import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.WriteBufferWaterMark
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.GracefullyShutdown
import play.net.netty.NettyServerBuilder
import play.net.netty.createEventLoopGroup
import play.net.netty.toPlay

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class NettyServerConfiguration {

  companion object : KLogging()

  @Bean("bossEventLoopGroup")
  fun bossEventLoopGroup(shutdown: GracefullyShutdown): EventLoopGroup {
    val executor = createEventLoopGroup("netty-boss", 1)
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_ACCEPTOR, GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_ACCEPTOR, executor
    ) {
      it.shutdownGracefully().toPlay()
    }
    return executor
  }

  @Bean("workerEventLoopGroup")
  fun workerEventLoopGroup(shutdown: GracefullyShutdown): EventLoopGroup {
    val executor = createEventLoopGroup("netty-worker", 0)
    shutdown.addTask(
      GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_WORKER, GracefullyShutdown.PHASE_SHUTDOWN_NETWORK_WORKER, executor
    ) {
      it.shutdownGracefully().toPlay()
    }
    return executor
  }

  @Bean
  fun nettyServerBuilder(
    @Qualifier("bossEventLoopGroup") parent: EventLoopGroup,
    @Qualifier("workerEventLoopGroup") child: EventLoopGroup,
  ): NettyServerBuilder {
    return NettyServerBuilder().eventLoopGroup(parent, child).option(ChannelOption.SO_REUSEADDR, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
      .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
      .childOption(ChannelOption.SO_RCVBUF, 8 * 1024).childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
  }
}
