package play.example.game.app.module.reward.res

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.validation.Valid
import play.example.game.app.module.reward.model.Cost
import play.util.json.Json

data class RawCost(@field:JsonValue @field:Valid val reward: RawReward) {
  companion object {
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): RawCost {
      val reward = Json.convert(node, RawReward::class.java)
      return RawCost(reward)
    }
  }

  fun toCost(args: Map<String, Any>): Cost {
    return Cost(reward.toReward(args))
  }
}
