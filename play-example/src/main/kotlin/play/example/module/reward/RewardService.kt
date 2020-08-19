package play.example.module.reward

import com.google.common.collect.Collections2
import play.example.module.StatusCode
import play.example.module.mail.MailService
import play.example.module.player.Self
import play.example.module.reward.model.*
import play.example.module.reward.processor.RewardProcessor
import play.util.collection.asList
import play.util.control.Result2
import play.util.control.err
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardService @Inject constructor(private val mailService: MailService) {

  private val processors = mapOf<RewardType, RewardProcessor<Reward>>()

  fun merge(rewards: Collection<Reward>): List<Reward> {
    return merge(rewards, false)
  }

  private fun merge(rewards: Collection<Reward>, isCost: Boolean): List<Reward> {
    val size = rewards.size;
    return when (rewards.size) {
      0 -> emptyList()
      1 -> if (rewards is List) rewards else rewards.first().asList()
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

  fun tryReward(
    self: Self,
    reward: Reward,
    source: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    return tryReward(self, reward.asList(), source, bagFullStrategy, checkFcm)
  }

  fun tryReward(
    self: Self,
    rewards: Collection<Reward>,
    source: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    val rewardList = merge(rewards)
    var errorCode = 0
    var usedBagSize = 0
    val resultList = ArrayList<TryRewardResult>(rewardList.size)
    for (i in rewardList.indices) {
      val reward = rewardList[i]
      if (reward.num < 1) {
        continue
      }
      val processor = processors[reward.type]
      if (processor == null) {
        errorCode = StatusCode.Failure
        break
      }
      val tryResult = processor.tryReward(self, reward, source, usedBagSize, bagFullStrategy, checkFcm)
      if (tryResult.hasValue()) {
        val result = tryResult.get()
        usedBagSize += result.usedBagSize
        if (result.rewardCount > 0 || tryResult.get().mailCount > 0) {
          resultList += result
        }
      } else {
        errorCode = tryResult.getErrorCode()
        break
      }
    }
    return if (errorCode == 0) ok(TryRewardResultSet(resultList, bagFullStrategy, source)) else err(errorCode)
  }

  @Suppress("MapGetWithNotNullAssertionOperator")
  fun execReward(self: Self, tryResultSet: TryRewardResultSet): List<RewardResult> {
    if (tryResultSet.results.isEmpty()) {
      return emptyList()
    }
    val source = tryResultSet.source
    val results = tryResultSet.results.map { processors[it.reward.type]!!.execReward(self, it, source) }
    if (tryResultSet.bagFullStrategy == BagFullStrategy.Mail) {
      val mailRewards = results.mapNotNull { it.mailReward }
      if (mailRewards.isNotEmpty()) {
        mailService.sendMail(self, 1, mailRewards, source)
      }
    }
    log(self, results, source)
    return results
  }

  fun tryCost(self: Self, cost: Cost, source: Int): Result2<TryCostResultSet> {
    return tryCost(self, cost.asList(), source)
  }

  fun tryCost(self: Self, costs: Collection<Cost>, source: Int): Result2<TryCostResultSet> {
    val costList = merge(Collections2.transform(costs) { it?.reward }, true)
    val resultList = ArrayList<TryCostResult>(costList.size)
    var errorCode = 0
    for (i in costList.indices) {
      val cost = costList[i]
      if (cost.num < 1) {
        continue
      }
      val processor = processors[cost.type]
      if (processor == null) {
        errorCode = 1 // TODO
        continue
      }
      val tryResult = processor.tryCost(self, cost, source)
      if (tryResult.hasValue()) {
        tryResult.get().appendTo(resultList)
      } else {
        errorCode = tryResult.getErrorCode()
        break
      }
    }
    return if (errorCode == 0) ok(TryCostResultSet(resultList, source)) else err(errorCode)
  }

  fun execCost(self: Self, tryResultSet: TryCostResultSet): List<CostResult> {
    val source = tryResultSet.source
    val results = tryResultSet.results.map { processors[it.cost.type]!!.execCost(self, it, source) }
    log(self, results, source)
    return results
  }

  private fun log(self: Self, results: Collection<RewardOrCostResult>, source: Int) {
    // TODO
  }
}
