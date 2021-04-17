package play.example.game.module.reward

import play.example.game.module.player.Self
import play.example.game.module.player.event.PlayerEventListener
import play.example.game.module.player.event.PlayerEventReceive
import play.example.game.module.player.event.PlayerEventReceiveBuilder
import play.example.game.module.player.event.PlayerExecCost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 玩家的奖励/消耗处理
 * @author LiangZengle
 */
@Singleton
class PlayerRewardService @Inject constructor(private val rewardService: RewardService) : PlayerEventListener {
  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match(::execCost)
      .build()
  }

  private fun execCost(self: Self, event: PlayerExecCost) {
    event.promise.complete(runCatching { rewardService.tryAndExecCost(self, event.costs, event.logSource) })
  }
}
