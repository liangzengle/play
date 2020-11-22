package play.example.module.reward

import com.google.common.collect.Collections2
import play.example.module.reward.model.Cost
import play.example.module.reward.model.Reward

/**
 *
 * @author LiangZengle
 */
object RewardHelper {

  @JvmStatic
  fun mergeReward(rewards: Collection<Reward>): List<Reward> {
    return merge(rewards, false)
  }

  @JvmStatic
  fun mergeCost(costs: Collection<Cost>): List<Reward> {
    return merge(Collections2.transform(costs) { it!!.reward }, true)
  }

  @JvmStatic
  private fun merge(rewards: Collection<Reward>, isCost: Boolean): List<Reward> {
    val size = rewards.size
    return when (rewards.size) {
      0 -> emptyList()
      1 -> if (rewards is List) rewards else listOf(rewards.first())
      else -> {
        val merged = ArrayList<Reward>(size)
        rewards.forEach { r ->
          if (r.num > 0) {
            val i = merged.indexOfFirst { it.canMerge(r, isCost) }
            if (i == -1) merged += r
            else merged[i] = merged[i] + r.num
          }
        }
        merged
      }
    }
  }
}
