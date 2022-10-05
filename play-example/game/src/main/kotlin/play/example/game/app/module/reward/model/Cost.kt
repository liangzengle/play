package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import play.util.json.Json
import kotlin.math.ceil

@JvmRecord
data class Cost(@field:JsonValue @field:Valid @JvmField val reward: Reward) {
  companion object {
    @JvmStatic
    private fun fromJson(jsonNode: JsonNode): Cost {
      val reward = Json.convert(jsonNode, Reward::class.java)
      return Cost(reward)
    }
  }

  operator fun plus(count: Long) = Cost(reward + count)

  operator fun minus(count: Long) = Cost(reward - count)

  operator fun times(count: Long) = Cost(reward * count)

  operator fun times(count: Double) = Cost(reward.copy(num = ceil(reward.num * count).toLong()))

  operator fun div(count: Long) = div(count.toDouble())

  operator fun div(count: Double) = Cost(reward.copy(num = ceil(reward.num / count).toLong()))

  val id: Int get() = reward.id

  val num: Long get() = reward.num

  override fun toString(): String {
    return reward.toString()
  }

  fun toCostList(): CostList {
    return CostList(this)
  }
}
