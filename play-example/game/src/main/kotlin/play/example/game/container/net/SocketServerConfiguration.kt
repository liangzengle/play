package play.example.game.container.net

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import com.google.common.net.HostAndPort
import com.typesafe.config.Config
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.WriteBufferWaterMark
import io.netty.handler.flush.FlushConsolidationHandler
import io.netty.handler.timeout.IdleStateHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.akka.ActorConfigurationSupport
import play.akka.GuardianBehavior
import play.example.game.container.net.codec.RequestDecoder
import play.example.game.container.net.codec.ResponseEncoder
import play.example.game.container.net.handler.HeartbeatResponder
import play.example.game.container.net.handler.RequestIdValidator
import play.example.game.container.net.handler.RequestsPerSecondController
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder
import play.net.netty.handler.ScheduledFlushConsolidationHandler
import play.netty.NettyConfiguration
import java.util.concurrent.TimeUnit

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class SocketServerConfiguration : ActorConfigurationSupport {

  @Bean
  fun nettyServerBuilder(
    @Qualifier(NettyConfiguration.NETTY_SELECTOR) parent: EventLoopGroup,
    @Qualifier(NettyConfiguration.NETTY_IO) child: EventLoopGroup,
  ): NettyServerBuilder {
    return NettyServerBuilder().eventLoopGroup(parent, child).option(ChannelOption.SO_REUSEADDR, true)
      .childOption(ChannelOption.TCP_NODELAY, true)
      .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
      .childOption(ChannelOption.SO_RCVBUF, 8 * 1024).childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
  }

  @Bean
  fun address(conf: Config): HostAndPort {
    val host = conf.getString("play.net.host")
    val port = conf.getInt("play.net.port")
    return HostAndPort.fromParts(host, port)
  }

  @Bean("gameSocketServer")
  fun socketServer(
    conf: Config, serverBuilder: NettyServerBuilder, sessionManager: ActorRef<SessionManager.Command>
  ): NettyServer {
    val host = conf.getString("play.net.host")
    val port = conf.getInt("play.net.port")
    val requestsPerSecond = conf.getInt("play.net.requests-per-second")
    val readTimeout = conf.getDuration("play.net.read-timeout")
    return serverBuilder.host(host).port(port).childHandler { ch ->
      ch.pipeline().apply {
        addLast(ResponseEncoder)
        addLast(RequestDecoder(1024))
        if (requestsPerSecond > 0) {
          addLast(RequestsPerSecondController(requestsPerSecond))
        }
        addLast(RequestIdValidator())
        addLast(
          ScheduledFlushConsolidationHandler(
            FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true, 50
          )
        )
        addLast(IdleStateHandler(readTimeout.toMillis(), 0, 0, TimeUnit.MILLISECONDS))
        addLast(HeartbeatResponder)
      }
      ch.config().isAutoRead = false
      sessionManager.tell(SessionManager.CreateSession(ch))
    }.build("game")
  }

  @Bean
  fun sessionManager(actorSystem: ActorSystem<GuardianBehavior.Command>): ActorRef<SessionManager.Command> {
    return spawn(actorSystem, SessionManager.create(), "SessionManager")
  }
}
