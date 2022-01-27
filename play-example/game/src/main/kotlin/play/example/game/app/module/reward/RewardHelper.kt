package play.example.game.app.module.reward

import com.google.common.collect.Collections2
import com.google.common.collect.Lists
import play.example.game.app.module.reward.model.Cost
import play.example.game.app.module.reward.model.Reward

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
  fun mergeCost(costs: Collection<Cost>): List<Cost> {
    return Lists.transform(merge(Collections2.transform(costs) { it!!.reward }, true), ::Cost)
  }

  @JvmStatic
  private fun merge(rewards: Collection<Reward>, isCost: Boolean): List<Reward> {
    return when (val size = rewards.size) {
      0 -> emptyList()
      1 -> {
        val reward = rewards.first()
        if (reward.num <= 0) {
          emptyList()
        } else if (rewards is List) {
          rewards
        } else {
          listOf(reward)
        }
      }
      else -> {
        val merged = ArrayList<Reward>(size)
        for (r in rewards) {
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
