package play.example.game.app.module.item

import org.springframework.stereotype.Component
import play.Order
import play.Orders
import play.example.common.StatusCode
import play.example.game.app.module.item.res.ItemResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.reward.RewardProcessor
import play.example.game.app.module.reward.model.*
import play.util.control.Result2
import play.util.control.ok

/**
 *
 * @author LiangZengle
 */
@Component
@Order(Orders.Lowest)
class ItemRewardProcessor : RewardProcessor {

  override fun support(reward: Reward): Boolean {
    return ItemResourceSet.contains(reward.id)
  }

  override fun tryReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult> {
    // TODO
    if (!ItemResourceSet.contains(reward.id)) {
      return StatusCode.ResourceNotFound
    }
    return ok(TryRewardResult(reward, usedBagSize, 0, 0))
  }

  override fun execReward(self: Self, tryRewardResult: TryRewardResult, logSource: Int): RewardResult {
    return RewardResult(tryRewardResult, 0)
  }

  override fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSetLike> {
    // TODO
    return StatusCode.Failure
  }

  override fun execCost(self: Self, tryCostResult: TryCostResult, logSource: Int): CostResult {
    return CostResult(tryCostResult, 0)
  }
}
