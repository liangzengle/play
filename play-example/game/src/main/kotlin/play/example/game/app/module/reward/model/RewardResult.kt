package play.example.game.app.module.reward.model

sealed class RewardOrCostResult {

  /** 奖励 or 消耗 */
  abstract val reward: Reward

  /** 应变化 */
  abstract val changeCount0: Long

  /** 实际变化 */
  abstract val changeCount: Long

  /** 通过邮件发放的部分 */
  abstract val mailReward: Reward?

  /** 变化后的数量，仅变化内容为货币时该值有效 */
  abstract val currentValue: Long
}

class RewardResult(val tryResult: TryRewardResult, override val currentValue: Long) : RewardOrCostResult() {
  override val reward: Reward get() = tryResult.reward
  override val changeCount0 get() = tryResult.reward.num
  override val changeCount get() = tryResult.rewardCount
  override val mailReward = tryResult.mailReward
}

class CostResult(val tryResult: TryCostResult, override val currentValue: Long) : RewardOrCostResult() {
  override val reward: Reward get() = tryResult.cost
  override val changeCount0 get() = -tryResult.cost.num
  override val changeCount get() = -tryResult.costCount
  override val mailReward: Reward? = null
}

class CostResultSet(val results: List<CostResult>)

class RewardResultSet(val results: List<RewardResult>)
