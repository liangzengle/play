package play.example.module.reward.model

abstract class RewardOrCostResult {
  abstract val rewardType: RewardType
  abstract val originalCount: Int
  abstract val actualCount: Int
  abstract val mailReward: Reward?
  abstract val mailCount: Int
}

abstract class RewardResult(val tryResult: TryRewardResult) : RewardOrCostResult() {
  override val rewardType get() = tryResult.reward.type
  override val originalCount get() = tryResult.reward.num
  override val actualCount get() = tryResult.rewardCount
  override val mailReward = tryResult.mailReward
  override val mailCount = tryResult.mailReward?.num ?: 0
}

class CurrencyRewardResult(tryResult: TryRewardResult, val currentValue: Long) : RewardResult(tryResult)

class ItemLikeRewardResult(tryResult: TryRewardResult) : RewardResult(tryResult) {
  val cfgId get() = (tryResult.reward as ItemLikeReward).cfgId
}

abstract class CostResult(val tryResult: TryCostResult) : RewardOrCostResult() {
  override val rewardType get() = tryResult.cost.type
  override val originalCount get() = -tryResult.cost.num
  override val actualCount get() = -tryResult.costCount
  override val mailReward: Reward? = null
  override val mailCount = 0
}

class CurrencyCostResult(tryResult: TryCostResult, val currentValue: Long) : CostResult(tryResult)

class ItemLikeCostResult(tryResult: TryCostResult) : CostResult(tryResult)
