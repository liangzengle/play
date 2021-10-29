package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import play.util.json.Json
import kotlin.math.ceil

data class Cost(@field:JsonValue @field:Valid val reward: Reward) {
  companion object {
    @JvmStatic
    private fun fromJson(jsonNode: JsonNode): Cost {
      val reward = Json.convert(jsonNode, Reward::class.java)
      return Cost(reward)
    }
  }

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

  fun toCostList(): CostList {
    return CostList(this)
  }
}

//data class StepCost(private val reward: Reward, private val step: Int, private val sign: Char = '+') {
//
//  fun getCost(stepped: Int): Cost {
//    if (stepped == 0 || step == 0) {
//      return Cost(reward)
//    }
//    if (sign == '+') {
//      return Cost(reward.plus(stepped * step))
//    }
//    if (sign == '*') {
//      return Cost(reward.plus(step.toDouble().pow(stepped.toDouble()).toInt()))
//    }
//    throw IllegalStateException("should not happen.")
//  }
//
//  companion object {
//    @JvmStatic
//    @JsonCreator
//    private fun fromJson(node: ObjectNode): StepCost {
//      val step = node.get("step")?.intValue() ?: 0
//      var sign = node.get("sign")?.intValue()?.toChar() ?: '+'
//      if (sign != '+' || sign != '*') {
//        sign = ModeDependent.logAndRecover(IllegalStateException("递增消耗运算符错误: $node"), '+') { "递增消耗运算符错误: $sign" }
//      }
//      val reward = Json.convert(node, Reward::class.java)
//      return StepCost(reward, step, sign)
//    }
//  }
//}
