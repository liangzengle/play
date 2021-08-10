package play.example.game.container.net

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.common.akka.ActorConfigurationSupport
import play.example.common.akka.GuardianBehavior
import play.example.game.container.net.codec.RequestDecoder
import play.example.game.container.net.codec.ResponseEncoder
import play.net.netty.NettyServer
import play.net.netty.NettyServerBuilder

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

  @Bean
  fun sessionManager(actorSystem: ActorSystem<GuardianBehavior.Command>): ActorRef<SessionManager.Command> {
    return spawn(actorSystem, SessionManager.create(), "SessionManager")
  }
}
