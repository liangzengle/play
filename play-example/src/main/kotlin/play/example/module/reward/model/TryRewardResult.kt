package play.example.module.reward.model

data class TryRewardResult(val reward: Reward, val usedBagSize: Int, val changeCount: Int, val mailCount: Int) {

  val rewardCount = reward.num + changeCount - mailCount

  val mailReward: Reward? = if (mailCount > 0) reward.copy(mailCount) else null
}

data class TryRewardResultSet(val results: List<TryRewardResult>, val bagFullStrategy: BagFullStrategy, val source: Int)
