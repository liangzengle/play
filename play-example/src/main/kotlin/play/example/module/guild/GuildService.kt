package play.example.module.guild

import akka.actor.typed.ActorRef
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.example.module.guild.entity.GuildEntityCache

/**
 * 工会模块逻辑处理
 */
@Singleton
class GuildService @Inject constructor(
  private val guildManager: Provider<ActorRef<GuildManager.Command>>,
  private val guildEntityCache: GuildEntityCache
) {

  fun hasGuild(playerId: Long): Boolean = getPlayerGuildId(playerId).isPresent

  fun getPlayerGuildId(playerId: Long): OptionalLong {
    return GuildManager.getPlayerGuildId(playerId)
  }
}
