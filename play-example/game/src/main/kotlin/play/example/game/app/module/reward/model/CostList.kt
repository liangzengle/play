package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import jakarta.validation.Valid
import org.eclipse.collections.api.factory.primitive.IntLongMaps
import org.eclipse.collectionx.toJava
import play.example.game.app.module.reward.RewardHelper
import play.util.json.Json

class CostList private constructor(
  @field:Valid @field:JsonValue private val costs: ImmutableList<Cost>
) {
  companion object {
    @JvmStatic
    val Empty = CostList(ImmutableList.of())

    @JvmStatic
    @JsonCreator
    private fun fromJson(jsonNode: JsonNode): CostList {
      val rewards = if (jsonNode is TextNode) {
        val textValue = jsonNode.textValue()
        RewardHelper.parseRewardString(textValue)
      } else {
        Json.convert(jsonNode, jacksonTypeRef())
      }
      return CostList(RewardHelper.mergeCost(Lists.transform(rewards, ::Cost)))
    }

    @JvmStatic
    fun merge(list1: CostList, list2: CostList): CostList {
      if (list1.isEmpty()) {
        return list2
      }
      if (list2.isEmpty()) {
        return list1
      }
      val base: MutableList<Reward>
      val toBeMerged: List<Reward>
      if (list2.size() > list1.size()) {
        base = ArrayList(list2.asRewardList())
        toBeMerged = list1.asRewardList()
      } else {
        base = ArrayList(list1.asRewardList())
        toBeMerged = list2.asRewardList()
      }
      for (r in toBeMerged) {
        if (r.num > 0) {
          val i = base.indexOfFirst { it.canMerge(r, true) }
          if (i == -1) base += r
          else base[i] = base[i] + r.num
        }
      }
      return CostList(ImmutableList.copyOf(Lists.transform(base, ::Cost)))
    }

    @JvmName("of")
    @JvmStatic
    operator fun invoke(cost: Cost): CostList {
      return if (cost.num > 0) CostList(ImmutableList.of(cost)) else Empty
    }

    @JvmName("of")
    @JvmStatic
    operator fun invoke(costs: List<Cost>): CostList {
      return if (costs.isEmpty()) Empty else CostList(RewardHelper.mergeCost(costs))
    }
  }

  operator fun plus(that: CostList): CostList {
    return merge(this, that)
  }

  private fun asRewardList(): List<Reward> = Lists.transform(costs) { it!!.reward }

  /**
   * @return the underlying immutable map
   */
  fun asList(): List<Cost> = costs

  /**
   * @return convert to a map which key is id and value is num
   */
  fun toMap(): Map<Int, Long> = IntLongMaps.immutable.from(costs, { it.id }, { it.num }).toJava()

  fun isEmpty() = costs.isEmpty()

  fun size() = costs.size
}
