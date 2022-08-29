package play.example.game.app.module.guild

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.app.module.guild.entity.GuildEntityCache
import play.example.game.app.module.player.PlayerRequestHandler
import play.example.game.app.module.player.PlayerService
import play.example.game.container.gs.GameServerScopeConfiguration
import play.util.classOf

@Configuration(proxyBeanMethods = false)
class GuildSpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun guildManager(
    playerRequestHandler: PlayerRequestHandler,
    guildEntityCache: GuildEntityCache,
    playerService: PlayerService,
    guildCache: GuildCache
  ): ActorRef<GuildManager.Command> {
    return spawn("GuildManager", classOf()) {
      GuildManager.create(
        playerRequestHandler,
        guildEntityCache,
        playerService,
        guildCache
      )
    }
  }
}
