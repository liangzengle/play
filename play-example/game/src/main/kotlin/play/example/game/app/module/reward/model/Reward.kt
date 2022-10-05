package play.example.game.app.module.reward.model

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import play.example.game.app.module.item.res.ItemResource
import play.res.validation.constraints.ReferTo

@JvmRecord
data class Reward(
  @JvmField @field:Positive @field:ReferTo(ItemResource::class)
  val id: Int,
  @JvmField @field:PositiveOrZero
  val num: Long
) {
  init {
    require(num >= 0) { "num must be non-negative: $num" }
  }

  fun toRewardList(): RewardList = RewardList(this)

  operator fun plus(count: Long) = copy(num = Math.addExact(num, count))

  operator fun minus(count: Long) = copy(num = Math.subtractExact(num, count))

  operator fun times(count: Long) = copy(num = Math.multiplyExact(num, count))

  operator fun times(count: Double) = copy(num = (num * count).toLong())

  operator fun div(count: Long) = copy(num = Math.addExact(num, count))

  operator fun div(count: Double) = copy(num = (num / count).toLong())

  @Suppress("UNUSED_PARAMETER")
  fun canMerge(other: Reward, isCost: Boolean = false): Boolean {
    return this.id == other.id
  }
}
