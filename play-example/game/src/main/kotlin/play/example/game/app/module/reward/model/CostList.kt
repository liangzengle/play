package play.example.game.app.module.reward.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import play.example.game.app.module.reward.RewardHelper
import play.util.json.Json
import javax.validation.Valid

class CostList private constructor(
  @field:Valid
  @field:JsonValue
  private val costs: ImmutableList<Cost>
) {
  companion object {
    @JvmStatic
    val Empty = CostList(ImmutableList.of())

    @JvmStatic
    @JsonCreator
    private fun fromJson(jsonNode: JsonNode): CostList {
      val type = Json.mapper.typeFactory.constructCollectionType(ImmutableList::class.java, Cost::class.java)
      val costs = Json.mapper.convertValue<ImmutableList<Cost>>(jsonNode, type)
      return CostList(costs)
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
      return CostList(ImmutableList.copyOf(Lists.transform(base) { Cost(it!!) }))
    }

    @JvmName("of")
    @JvmStatic
    operator fun invoke(cost: Cost): CostList = CostList(ImmutableList.of(cost))

    @JvmName("of")
    @JvmStatic
    operator fun invoke(costs: List<Cost>): CostList =
      CostList(ImmutableList.copyOf(Lists.transform(RewardHelper.mergeCost(costs)) { Cost(it!!) }))
  }

  operator fun plus(that: CostList): CostList {
    return merge(this, that)
  }

  private fun asRewardList(): List<Reward> = Lists.transform(costs) { it!!.reward }

  /**
   * @return a read-only List
   */
  fun asList(): List<Cost> = costs

  fun isEmpty() = costs.isEmpty()

  fun size() = costs.size
}
