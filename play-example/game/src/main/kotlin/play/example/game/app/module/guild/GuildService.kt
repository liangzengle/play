package play.example.game.app.module.guild

import akka.actor.typed.ActorRef
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import play.example.game.app.module.guild.entity.GuildEntityCache
import play.util.unsafeLazy
import java.util.*

/**
 * 工会模块逻辑处理
 */
@Component
class GuildService(
  private val guildManagerProvider: ObjectProvider<ActorRef<GuildManager.Command>>,
  private val guildEntityCache: GuildEntityCache,
  private val guildCache: GuildCache
) {

  private val guildManager by unsafeLazy { guildManagerProvider.getObject() }

  fun hasGuild(playerId: Long): Boolean = getPlayerGuildId(playerId).isPresent

  fun getPlayerGuildId(playerId: Long): OptionalLong {
    return guildCache.getPlayerGuildId(playerId)
  }
}
