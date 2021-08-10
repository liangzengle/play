package play.example.game.app.module.guild

import akka.actor.typed.ActorRef
import play.example.game.app.module.guild.entity.GuildEntityCache
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 工会模块逻辑处理
 */
@Singleton
@Named
class GuildService @Inject constructor(
  private val guildManager: Provider<ActorRef<GuildManager.Command>>,
  private val guildEntityCache: GuildEntityCache
) {

  fun hasGuild(playerId: Long): Boolean = getPlayerGuildId(playerId).isPresent

  fun getPlayerGuildId(playerId: Long): OptionalLong {
    return GuildManager.getPlayerGuildId(playerId)
  }
}
