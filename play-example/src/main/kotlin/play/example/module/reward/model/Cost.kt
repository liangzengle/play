package play.example.module.reward.model

data class Cost(val reward: Reward) {
  operator fun plus(count: Int) = Cost(reward + count)

  operator fun minus(count: Int) = Cost(reward - count)

  operator fun times(count: Int) = Cost(reward * count)

  operator fun times(count: Double) = Cost(reward * count)

  operator fun div(count: Int) = Cost(reward / count)

  operator fun div(count: Double) = Cost(reward / count)

  val type: RewardType get() = reward.type

  val num: Int get() = reward.num
}
