package play.example.module.guild

import akka.actor.typed.ActorRef
import play.example.common.net.message.of
import play.example.module.guild.domain.GuildErrorCode
import play.example.module.guild.entity.GuildEntityCache
import play.example.module.guild.message.GuildProto
import play.mvc.RequestResult
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import play.util.control.Result2
import play.util.control.err
import play.util.control.ok
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 工会模块逻辑处理
 */
@Singleton
class GuildService @Inject constructor(
  private val guildManager: Provider<ActorRef<GuildManager.Command>>,
  private val guildEntityCache: GuildEntityCache
) {

  /**
   * 创建工会
   *
   * @param playerId 玩家id
   * @param guildName 工会名称
   * @return PlayFuture<RequestResult<GuildProto>>
   */
  // 由GuildManager调用
  fun createGuild(playerId: Long, guildName: String): PlayFuture<RequestResult<GuildProto>> {
    val promise = PlayPromise.make<Result2<GuildProto>>()
    guildManager.get().tell(GuildManager.CreateGuildRequest(playerId, guildName, promise))
    return promise.future.map { RequestResult.of { it } }
  }

  /**
   * 加入工会
   * @param playerId 玩家id
   * @param guildId 工会id
   * @return Result2<GuildProto>
   */
  // 由GuildManager调用
  fun joinGuild(playerId: Long, guildId: Long): Result2<GuildProto> {
    if (hasGuild(playerId)) {
      return err(GuildErrorCode.HasGuild)
    }
    // 所有工会都在缓存中，不在说明没有这个工会，没必要读数据库
    val maybeGuild = guildEntityCache.getCached(guildId)
    if (maybeGuild.isEmpty) {
      return err(GuildErrorCode.GuildNotExists)
    }
    val guild = maybeGuild.get()
    guild.members.add(playerId)
    GuildManager.updatePlayerGuild(playerId, guildId)
    return ok(guild.toProto())
  }

  fun hasGuild(playerId: Long): Boolean = getPlayerGuildId(playerId).isPresent

  fun getPlayerGuildId(playerId: Long): OptionalLong {
    return GuildManager.getPlayerGuildId(playerId)
  }
}
