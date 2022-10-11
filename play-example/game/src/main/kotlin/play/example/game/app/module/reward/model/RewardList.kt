package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.google.common.collect.ImmutableList
import jakarta.validation.Valid
import org.eclipse.collections.api.factory.primitive.IntLongMaps
import org.eclipse.collectionx.toJava
import play.example.game.app.module.reward.RewardHelper
import play.util.json.Json

class RewardList private constructor(
  @field:Valid
  @field:JsonValue
  private val rewards: ImmutableList<Reward>
) {

  companion object {
    @JvmStatic
    val Empty = RewardList(ImmutableList.of())

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
    operator fun invoke(rewards: List<Reward>): RewardList {
      return if (rewards.isEmpty()) Empty else RewardList(ImmutableList.copyOf(RewardHelper.mergeReward(rewards)))
    }
  }

  operator fun plus(that: RewardList): RewardList {
    return RewardHelper.merge(this, that)
  }

  /**
   * @return the underlying immutable list
   */
  fun asList(): List<Reward> = rewards

  /**
   * @return convert to a map which key is id and value is num
   */
  fun toMap(): Map<Int, Long> = IntLongMaps.immutable.from(rewards, { it.id }, { it.num }).toJava()

  fun isEmpty() = rewards.isEmpty()

  fun isNotEmpty() = rewards.isNotEmpty()

  fun size() = rewards.size

  override fun toString(): String {
    return rewards.toString()
  }
}
