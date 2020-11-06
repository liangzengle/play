package play.example.module.guild

import akka.actor.typed.ActorRef
import play.example.module.guild.config.GuildSettingConf
import play.example.module.guild.domain.GuildLogSource
import play.example.module.guild.message.GuildProto
import play.example.module.player.event.PlayerEventBus
import play.example.module.player.event.PlayerExecCost
import play.example.module.reward.model.CostResultSet
import play.mvc.RequestResult
import play.util.concurrent.PlayFuture
import play.util.concurrent.Promise
import play.util.control.Result2
import play.util.control.getCause
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 工会模块逻辑处理
 */
@Singleton
public class GuildService @Inject constructor(
  private val playerEventBus: PlayerEventBus,
  private val guildManager: Provider<ActorRef<GuildManager.Command>>
) {

  fun createGuild(playerId: Long, guildName: String): PlayFuture<RequestResult<GuildProto>> {
    // TODO check name
    // TODO check player guild
    // 由PlayerActor执行消耗
    val costPromise = Promise.make<Result2<CostResultSet>>()
    val costRequest =
      PlayerExecCost(playerId, GuildSettingConf.createCost, GuildLogSource.createGuild, costPromise)
    playerEventBus.post(costRequest)

    val createPromise = Promise.make<RequestResult<GuildProto>>()
    costPromise.future.onComplete {
      if (it.isFailure) {
        createPromise.failure(it.getCause())
      } else {
        val result = it.getOrThrow()
        if (result.isErr()) { // 消耗失败, 返回失败结果
          createPromise.success(RequestResult.Code(result.getErrorCode()))
        } else {
          // 消耗成功，由GuildManager创建工会
          guildManager.get().tell(GuildManager.CreateGuildCommand(playerId, guildName, createPromise))
        }
      }
    }
    return createPromise.future
  }
}
