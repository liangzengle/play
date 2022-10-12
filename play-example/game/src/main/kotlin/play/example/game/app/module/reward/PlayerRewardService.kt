package play.example.game.app.module.reward

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.PlayerExecCost
import play.example.game.app.module.player.event.subscribe
import play.example.game.app.module.reward.model.CostList
import play.util.control.peek

/**
 * 玩家的奖励/消耗处理
 * @author LiangZengle
 */
@Component
class PlayerRewardService(private val rewardService: RewardService, eventBus: PlayerEventBus) {

  init {
    eventBus.subscribe(::execCost)
  }

  private fun execCost(self: Self, event: PlayerExecCost) {
    event.promise.catchingComplete {
      rewardService.tryAndExecCost(self, CostList(event.costs), event.logSource).peek {
        // TODO
//          SessionWriter.write(self.id, it)
      }
    }
  }
}
