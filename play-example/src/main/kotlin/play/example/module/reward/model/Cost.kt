package play.example.module.reward.model

import com.fasterxml.jackson.annotation.JsonValue
import kotlin.math.ceil

data class Cost(@field:JsonValue val reward: Reward) {
  operator fun plus(count: Int) = Cost(reward + count)

  operator fun minus(count: Int) = Cost(reward - count)

  operator fun times(count: Int) = Cost(reward * count)

  operator fun times(count: Double) = Cost(reward.copy(num = ceil(reward.num * count).toInt()))

  operator fun div(count: Int) = div(count.toDouble())

  operator fun div(count: Double) = Cost(reward.copy(num = ceil(reward.num / count).toInt()))

  val type: RewardType get() = reward.type

  val num: Int get() = reward.num

  override fun toString(): String {
    return reward.toString()
  }
}
