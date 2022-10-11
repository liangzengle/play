package play.example.game.app.module.reward

import com.google.common.base.Splitter
import com.google.common.collect.Collections2
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import org.eclipse.collections.impl.list.mutable.FastList
import play.example.game.app.module.reward.model.Cost
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.reward.res.RawReward

/**
 *
 * @author LiangZengle
 */
object RewardHelper {

  const val ElementSplitter = ';'
  const val AttributeSplitter = ','

  @JvmStatic
  fun mergeReward(rewards: Collection<Reward>): ImmutableList<Reward> {
    return merge(rewards, false)
  }

  @JvmStatic
  fun mergeCost(costs: Collection<Cost>): ImmutableList<Cost> {
    return ImmutableList.copyOf(Lists.transform(merge(Collections2.transform(costs) { it!!.reward }, true), ::Cost))
  }

  @JvmStatic
  private fun merge(rewards: Collection<Reward>, isCost: Boolean): ImmutableList<Reward> {
    return when (val size = rewards.size) {
      0 -> ImmutableList.of()
      1 -> {
        val reward = rewards.first()
        if (reward.num <= 0) {
          ImmutableList.of()
        } else if (rewards is List) {
          ImmutableList.copyOf(rewards)
        } else {
          ImmutableList.of(reward)
        }
      }

      else -> {
        val merged = FastList<Reward>(size)
        for (r in rewards) {
          if (r.num > 0) {
            val i = merged.indexOfFirst { it.canMerge(r, isCost) }
            if (i == -1) merged += r
            else merged[i] = merged[i] + r.num
          }
        }
        ImmutableList.copyOf(merged)
      }
    }
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
      base = FastList(list2.asList())
      toBeMerged = list1.asList()
    } else {
      base = FastList(list1.asList())
      toBeMerged = list2.asList()
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

  @JvmStatic
  fun parseRewardString(string: String): List<Reward> {
    return parseRewardString(string) { id, num -> Reward(id.toInt(), num.toLong()) }
  }

  @JvmStatic
  fun parseRewardStringAsRawRewards(string: String): List<RawReward> {
    return parseRewardString(string) { id, num -> RawReward(id.toInt(), num) }
  }

  @JvmStatic
  private fun <T : Any> parseRewardString(string: String, mapper: (String, String) -> T): List<T> {
    try {
      return Splitter.on(ElementSplitter).split(string)
        .asSequence()
        .filter { it.isNotBlank() }
        .map { element ->
          val iterator = Splitter.on(AttributeSplitter).split(element).iterator()
          val cfgId = iterator.next()
          val num = iterator.next()
          mapper(cfgId, num)
        }
        .toList()
    } catch (e: Exception) {
      throw IllegalArgumentException("奖励格式错误: $string", e)
    }
  }
}
