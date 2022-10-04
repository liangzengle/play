package play.example.game.app.module.reward.model

data class TryRewardResult(val reward: Reward, val usedBagSize: Int, val changeCount: Long, val mailCount: Long) {

  val rewardCount = reward.num + changeCount - mailCount

  val mailReward: Reward? = if (mailCount > 0) reward.copy(num = mailCount) else null
}

data class TryRewardResultSet(
  val results: List<TryRewardResult>,
  val bagFullStrategy: BagFullStrategy,
  val logSource: Int
)
