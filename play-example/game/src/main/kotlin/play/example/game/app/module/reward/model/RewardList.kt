package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.google.common.collect.ImmutableList
import jakarta.validation.Valid
import org.eclipse.collections.api.factory.Lists
import play.example.game.app.module.reward.RewardHelper
import play.util.json.Json

class RewardList private constructor(
  @field:Valid
  @field:JsonValue
  private val rewards: List<Reward>
) {

  companion object {
    @JvmStatic
    val Empty = RewardList(Lists.immutable.empty<Reward>().castToList())

    @JvmStatic
    @JsonCreator
    private fun fromJson(jsonNode: JsonNode): RewardList {
      val rewards = if (jsonNode is TextNode) {
        val textValue = jsonNode.textValue()
        RewardHelper.parseRewardString(textValue)
      } else {
        Json.convert(jsonNode, jacksonTypeRef())
      }
      return RewardList(RewardHelper.mergeReward(rewards))
    }

    @JvmName("of")
    @JvmStatic
    operator fun invoke(reward: Reward): RewardList {
      return if (reward.num > 0) RewardList(ImmutableList.of(reward)) else Empty
    }

    @JvmName("of")
    @JvmStatic
    operator fun invoke(rewards: List<Reward>): RewardList =
      RewardList(ImmutableList.copyOf(RewardHelper.mergeReward(rewards)))
  }

  operator fun plus(that: RewardList): RewardList {
    return RewardHelper.merge(this, that)
  }

  /**
   * @return a read-only List
   */
  fun asList(): List<Reward> = rewards

  fun isEmpty() = rewards.isEmpty()

  fun size() = rewards.size

  override fun toString(): String {
    return rewards.toString()
  }
}
