package play.example.game.app.module.guild

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.common.id.GameUIDGenerator
import play.example.game.app.module.guild.entity.GuildEntityCache
import play.example.game.app.module.server.res.ServerConfig
import play.spring.OrderedSmartInitializingSingleton
import play.util.collection.ConcurrentLongLongMap
import java.util.*

@Component
class GuildCache @Autowired constructor(
  private val guildEntityCache: GuildEntityCache,
  private val serverConfig: ServerConfig
) :
  OrderedSmartInitializingSingleton {

  private lateinit var playerIdToGuildId: ConcurrentLongLongMap

  private lateinit var guildIdGenerator: GameUIDGenerator

  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    playerIdToGuildId = guildEntityCache.getCachedEntities().fold(ConcurrentLongLongMap()) { r, entity ->
      for (member in entity.members) {
        r.put(member, entity.id)
      }
      r
    }

    guildIdGenerator = guildEntityCache.getCachedEntities().maxOfOrNull { it.id }?.let { GameUIDGenerator.fromId(it) }
      ?: serverConfig.newIdGenerator()
  }

  fun genGuildId(): Long {
    return guildIdGenerator.nextOrThrow()
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
