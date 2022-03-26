package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.*
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.player.condition.PlayerConditionService
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.model.*
import play.util.control.Result2

@Component
class PlayerServiceFacade(
  private val playerService: PlayerService,
  private val onlinePlayerService: OnlinePlayerService,
  private val playerConditionService: PlayerConditionService,
  private val rewardService: RewardService
) {

  fun isPlayerExists(playerId: Long) = playerService.isPlayerExists(playerId)

  fun isPlayerOnline(playerId: Long) = onlinePlayerService.isOnline(playerId)

  fun getPlayerNameOrNull(playerId: Long): String? = playerService.getPlayerNameOrNull(playerId)

  fun getPlayerNameOrEmpty(playerId: Long): String? = playerService.getPlayerNameOrElse(playerId, "")

  fun checkConditions(self: Self, conditions: Collection<PlayerCondition>): Result2<Nothing> {
    return playerConditionService.check(self, conditions)
  }

  fun tryAndExecReward(
    self: Self,
    rewardList: RewardList,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<RewardResultSet> {
    return rewardService.tryAndExecReward(self, rewardList, logSource, bagFullStrategy, checkFcm)
  }

  fun tryAndExecCost(self: Self, costList: CostList, logSource: Int): Result2<CostResultSet> {
    return rewardService.tryAndExecCost(self, costList, logSource)
  }
}
