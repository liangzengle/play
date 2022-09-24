package play.example.game.app.module.guild

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.common.id.UIDGenerator
import play.example.game.app.module.guild.entity.GuildEntityCache
import play.spring.OrderedSmartInitializingSingleton
import play.util.collection.ConcurrentLongLongMap
import java.util.*

@Component
class GuildCache @Autowired constructor(
  private val guildEntityCache: GuildEntityCache,
  private val idGenerator: UIDGenerator
) : OrderedSmartInitializingSingleton {

  private lateinit var playerIdToGuildId: ConcurrentLongLongMap

  override fun afterSingletonsInstantiated() {
    playerIdToGuildId = guildEntityCache.getAll().fold(ConcurrentLongLongMap()) { r, entity ->
      for (member in entity.members) {
        r.put(member, entity.id)
      }
      r
    }
  }

  fun genGuildId(): Long {
    return idGenerator.nextId()
  }

  fun hasGuild(playerId: Long) = playerIdToGuildId.containsKey(playerId)

  fun getPlayerGuildId(playerId: Long): OptionalLong {
    return playerIdToGuildId[playerId]?.let { OptionalLong.of(it) } ?: OptionalLong.empty()
  }

  fun updatePlayerGuild(playerId: Long, guildId: Long) {
    playerIdToGuildId.put(playerId, guildId)
  }

  fun changePlayerGuild(playerId: Long, fromGuildId: Long, toGuildId: Long) {
    if (playerIdToGuildId.remove(playerId, fromGuildId)) {
      playerIdToGuildId.put(playerId, toGuildId)
    } else {
      // TODO log
    }
  }
}
