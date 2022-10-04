package play.example.game.app.module.reward

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.reward.model.*
import play.util.control.Result2

interface RewardProcessor {

  /**
   * 是否支持处理该奖励
   *
   * @param reward 奖励
   * @return 是否支持
   */
  fun support(reward: Reward): Boolean

  /**
   * 奖励转换
   *
   * @param self
   * @param reward
   * @return null-不需要转换, 其他-转换后的奖励
   */
  fun transform(self: Self, reward: Reward): TransformedResult {
    return TransformedResult.Unchanged
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
  fun tryReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult>

  fun execReward(self: Self, tryRewardResult: TryRewardResult, logSource: Int): RewardResult

  fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSetLike>

  fun execCost(self: Self, tryCostResult: TryCostResult, logSource: Int): CostResult
}
