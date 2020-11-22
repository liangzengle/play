package play.example.module.reward

import play.example.module.StatusCode
import play.example.module.mail.MailService
import play.example.module.player.Self
import play.example.module.reward.exception.RewardProcessorNotFoundException
import play.example.module.reward.model.*
import play.example.module.reward.processor.RewardProcessor
import play.getLogger
import play.util.control.Result2
import play.util.control.err
import play.util.control.map
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardService @Inject constructor(private val mailService: MailService) {
  private val logger = getLogger()

  private val processors = mapOf<RewardType, RewardProcessor<Reward>>()

  fun tryReward(
    self: Self,
    reward: Reward,
    source: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    return tryReward(self, listOf(reward), source, bagFullStrategy, checkFcm)
  }

  fun tryReward(
    self: Self,
    rewards: Collection<Reward>,
    source: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    val rewardList = RewardHelper.mergeReward(rewards)
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
        logger.error(RewardProcessorNotFoundException(reward.type)) { "奖励预判异常: $reward" }
        continue
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
    return tryCost(self, listOf(cost), source)
  }

  fun tryCost(self: Self, costs: Collection<Cost>, source: Int): Result2<TryCostResultSet> {
    val costList = RewardHelper.mergeCost(costs)
    val resultList = ArrayList<TryCostResult>(costList.size)
    var errorCode = 0
    for (i in costList.indices) {
      val cost = costList[i]
      if (cost.num < 1) {
        continue
      }
      val processor = processors[cost.type]
      if (processor == null) {
        errorCode = StatusCode.Failure
        logger.error(RewardProcessorNotFoundException(cost.type)) { "消耗预判失败: $cost" }
        break
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

  fun execCost(self: Self, tryResultSet: TryCostResultSet): CostResultSet {
    val source = tryResultSet.source
    val results = tryResultSet.results.map {
      val processor = processors[it.cost.type] ?: error("should not happen.")
      processor.execCost(self, it, source)
    }
    log(self, results, source)
    return CostResultSet(results)
  }

  fun tryAndExecCost(self: Self, costs: Collection<Cost>, source: Int): Result2<CostResultSet> {
    if (costs.isEmpty()) return ok(CostResultSet(emptyList()))
    return tryCost(self, costs, source).map { execCost(self, it) }
  }

  private fun log(self: Self, results: List<RewardOrCostResult>, source: Int) {
    // TODO
  }
}
