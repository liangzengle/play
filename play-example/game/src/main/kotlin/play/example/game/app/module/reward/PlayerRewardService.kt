package play.example.game.app.module.reward

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.event.PlayerEventListener
import play.example.game.app.module.player.event.PlayerEventReceive
import play.example.game.app.module.player.event.PlayerEventReceiveBuilder
import play.example.game.app.module.player.event.PlayerExecCost
import play.example.game.app.module.reward.model.CostList
import play.util.control.peek

/**
 * 玩家的奖励/消耗处理
 * @author LiangZengle
 */
@Component
class PlayerRewardService(
  private val rewardService: RewardService
) : PlayerEventListener {
  override fun playerEventReceive(): PlayerEventReceive {
    return PlayerEventReceiveBuilder()
      .match(::execCost)
      .build()
  }

  private fun execCost(self: Self, event: PlayerExecCost) {
    event.promise.catchingComplete {
      rewardService.tryAndExecCost(self, CostList(event.costs), event.logSource)
        .peek {
          // TODO
//          SessionWriter.write(self.id, it)
        }
    }
  }
}
