package play.example.game.app.module.friend

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.app.module.player.PlayerRequestHandler
import play.example.game.container.gs.GameServerScopeConfiguration
import play.util.classOf

@Configuration(proxyBeanMethods = false)
class FriendSpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun guildManager(playerRequestHandler: PlayerRequestHandler): ActorRef<FriendManager.Command> {
    return spawn("FriendManager", classOf()) { FriendManager.create(playerRequestHandler) }
  }
}
