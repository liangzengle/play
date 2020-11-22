package play.example.module.reward

import play.example.module.player.Self
import play.example.module.player.event.PlayerEventListener
import play.example.module.player.event.PlayerEventReceive
import play.example.module.player.event.PlayerEventReceiveBuilder
import play.example.module.player.event.PlayerExecCost
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
