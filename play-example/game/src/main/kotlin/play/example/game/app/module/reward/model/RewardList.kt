package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.ImmutableList
import jakarta.validation.Valid
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
      if (jsonNode.isEmpty) {
        return Empty
      }
      val rewards = Json.convert<List<Reward>>(jsonNode)
      return RewardList(rewards)
    }

    @JvmStatic
    fun merge(list1: RewardList, list2: RewardList): RewardList {
      if (list1.isEmpty()) {
        return list2
      }
      if (list2.isEmpty()) {
        return list1
      }
      val base: MutableList<Reward>
      val toBeMerged: List<Reward>
      if (list2.size() > list1.size()) {
        base = ArrayList(list2.rewards)
        toBeMerged = list1.rewards
      } else {
        base = ArrayList(list1.rewards)
        toBeMerged = list2.rewards
      }
      for (r in toBeMerged) {
        if (r.num > 0) {
          val i = base.indexOfFirst { it.canMerge(r, false) }
          if (i == -1) base += r
          else base[i] = base[i] + r.num
        }
      }
      return RewardList(ImmutableList.copyOf(base))
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
    return merge(this, that)
  }

  /**
   * @return a read-only List
   */
  fun asList(): List<Reward> = rewards

  fun isEmpty() = rewards.isEmpty()

  fun size() = rewards.size
}
