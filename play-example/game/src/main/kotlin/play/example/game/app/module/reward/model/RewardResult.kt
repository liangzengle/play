package play.example.game.app.module.reward.model

abstract class RewardOrCostResult {
  abstract val reward: Reward
  abstract val originalCount: Long
  abstract val actualCount: Long
  abstract val mailReward: Reward?
  abstract val mailCount: Long
}

class RewardResult(val tryResult: TryRewardResult, val currentValue: Long) : RewardOrCostResult() {
  override val reward: Reward get() = tryResult.reward
  override val originalCount get() = tryResult.reward.num
  override val actualCount get() = tryResult.rewardCount
  override val mailReward = tryResult.mailReward
  override val mailCount = tryResult.mailReward?.num ?: 0
}

class CostResult(val tryResult: TryCostResult, val currentValue: Long) : RewardOrCostResult() {
  override val reward: Reward get() = tryResult.cost
  override val originalCount get() = -tryResult.cost.num
  override val actualCount get() = -tryResult.costCount
  override val mailReward: Reward? = null
  override val mailCount = 0L
}

class CostResultSet(val results: List<CostResult>)

class RewardResultSet(val results: List<RewardResult>)
