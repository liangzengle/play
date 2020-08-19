package play.example.module.reward.processor

import play.example.module.player.Self
import play.example.module.reward.model.*
import play.util.control.Result2

abstract class RewardProcessor<T : Reward> {

  abstract val rewardType: RewardType

  /**
   * 奖励预判
   *
   * @param self 当前玩家
   * @param reward 奖励
   * @param source 来源
   * @param usedBagSize 本次奖励已占用的背包格子
   * @param bagFullStrategy 背包满时的处理策略
   * @param checkFcm 是否计算防沉迷
   */
  abstract fun tryReward(
    self: Self,
    reward: T,
    source: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult>

  abstract fun execReward(self: Self, tryRewardResult: TryRewardResult, source: Int): RewardResult

  abstract fun tryCost(self: Self, cost: T, source: Int): Result2<TryCostResultSetLike>

  @Suppress("UNCHECKED_CAST")
  fun tryCost(self: Self, cost: Cost, source: Int): Result2<TryCostResultSetLike> {
    return tryCost(self, cost.reward as T, source)
  }

  abstract fun execCost(self: Self, tryCostResult: TryCostResult, source: Int): CostResult
}
