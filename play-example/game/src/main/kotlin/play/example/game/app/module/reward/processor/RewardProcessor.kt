package play.example.game.app.module.reward.processor

import play.example.game.app.module.player.Self
import play.example.game.app.module.reward.model.*
import play.util.control.Result2

abstract class RewardProcessor<T : Reward>(val rewardType: RewardType) {

  /**
   * 奖励转换
   *
   * @param self
   * @param reward
   * @return null-不需要转换, 其他-转换后的奖励
   */
  open fun transform(self: Self, reward: T): List<Reward>? {
    return null
  }

  /**
   * 奖励预判
   *
   * @param self 当前玩家
   * @param reward 奖励
   * @param logSource 来源
   * @param usedBagSize 本次奖励已占用的背包格子
   * @param bagFullStrategy 背包满时的处理策略
   * @param checkFcm 是否计算防沉迷
   */
  abstract fun tryReward(
    self: Self,
    reward: T,
    logSource: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult>

  abstract fun execReward(self: Self, tryRewardResult: TryRewardResult, logSource: Int): RewardResult

  abstract fun tryCost(self: Self, cost: T, logSource: Int): Result2<TryCostResultSetLike>

  @Suppress("UNCHECKED_CAST")
  fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSetLike> {
    return tryCost(self, cost.reward as T, logSource)
  }

  abstract fun execCost(self: Self, tryCostResult: TryCostResult, logSource: Int): CostResult
}
