package play.example.game.container.net

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import io.netty.handler.flush.FlushConsolidationHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.common.akka.ActorConfigurationSupport
import play.example.common.akka.GuardianBehavior
import play.example.game.container.net.codec.RequestDecoder
import play.example.game.container.net.codec.ResponseEncoder
import play.example.game.container.net.handler.RequestIdValidator
import play.example.game.container.net.handler.RequestsPerSecondController
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder
import play.net.netty.handler.ScheduledFlushConsolidationHandler

/**
 *
 * @author LiangZengle
 */
@Configuration
class SocketServerConfiguration : ActorConfigurationSupport {

  @Bean("gameSocketServer")
  fun socketServer(
    conf: Config,
    serverBuilder: NettyServerBuilder,
    sessionManager: ActorRef<SessionManager.Command>
  ): NettyServer {
    val host = conf.getString("play.net.host")
    val port = conf.getInt("play.net.port")
    val requestsPerSecond = conf.getInt("play.net.requests-per-second")
    return serverBuilder
      .host(host)
      .port(port)
      .childHandler { ch ->
        ch.pipeline().apply {
          addLast(ResponseEncoder)
          addLast(RequestDecoder(1024))
          if (requestsPerSecond > 0) {
            addLast(RequestsPerSecondController(requestsPerSecond))
          }
          addLast(RequestIdValidator())
          addLast(
            ScheduledFlushConsolidationHandler(
              FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES,
              true,
              50
            )
          )
        }
        ch.config().isAutoRead = false
        sessionManager.tell(SessionManager.CreateSession(ch))
      }
      .build("game")
  }

  @Bean
  fun sessionManager(actorSystem: ActorSystem<GuardianBehavior.Command>): ActorRef<SessionManager.Command> {
    return spawn(actorSystem, SessionManager.create(), "SessionManager")
  }
}
