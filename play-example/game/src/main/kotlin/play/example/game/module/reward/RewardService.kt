package play.example.game.module.reward

import javax.inject.Inject
import javax.inject.Singleton
import play.example.common.StatusCode
import play.example.game.module.mail.MailService
import play.example.game.module.player.Self
import play.example.game.module.reward.exception.RewardProcessorNotFoundException
import play.example.game.module.reward.model.*
import play.example.game.module.reward.processor.RewardProcessor
import play.util.collection.toImmutableEnumMap
import play.util.control.Result2
import play.util.control.err
import play.util.control.map
import play.util.control.ok
import play.util.logging.getLogger

@Singleton
class RewardService @Inject constructor(
  private val mailService: MailService,
  processorList: List<RewardProcessor<Reward>>
) {
  private val logger = getLogger()

  private val processors = processorList.toImmutableEnumMap { it.rewardType }

  fun tryReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    return tryReward(self, listOf(reward), logSource, bagFullStrategy, checkFcm)
  }

  fun tryReward(
    self: Self,
    rewards: Collection<Reward>,
    logSource: Int,
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
        errorCode = StatusCode.Failure.getErrorCode()
        logger.error(RewardProcessorNotFoundException(reward.type)) { "奖励预判异常: $reward" }
        continue
      }
      val tryResult = processor.tryReward(self, reward, logSource, usedBagSize, bagFullStrategy, checkFcm)
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
    return if (errorCode == 0) ok(TryRewardResultSet(resultList, bagFullStrategy, logSource)) else err(errorCode)
  }

  @Suppress("MapGetWithNotNullAssertionOperator")
  fun execReward(self: Self, tryResultSet: TryRewardResultSet): RewardResultSet {
    if (tryResultSet.results.isEmpty()) {
      return RewardResultSet(emptyList())
    }
    val logSource = tryResultSet.logSource
    val results = tryResultSet.results.map { processors[it.reward.type]!!.execReward(self, it, logSource) }
    if (tryResultSet.bagFullStrategy == BagFullStrategy.Mail) {
      val mailRewards = results.mapNotNull { it.mailReward }
      if (mailRewards.isNotEmpty()) {
        mailService.sendMail(self, 1, mailRewards, logSource)
      }
    }
    log(self, results, logSource)
    return RewardResultSet(results)
  }

  fun tryAndExecReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<RewardResultSet> {
    return tryReward(self, reward, logSource, bagFullStrategy, checkFcm).map { execReward(self, it) }
  }

  fun tryAndExecReward(
    self: Self,
    rewards: Collection<Reward>,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<RewardResultSet> {
    return tryReward(self, rewards, logSource, bagFullStrategy, checkFcm).map { execReward(self, it) }
  }

  fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSet> {
    return tryCost(self, listOf(cost), logSource)
  }

  fun tryCost(self: Self, costs: Collection<Cost>, logSource: Int): Result2<TryCostResultSet> {
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
        errorCode = StatusCode.Failure.getErrorCode()
        logger.error(RewardProcessorNotFoundException(cost.type)) { "消耗预判失败: $cost" }
        break
      }
      val tryResult = processor.tryCost(self, cost, logSource)
      if (tryResult.hasValue()) {
        tryResult.get().appendTo(resultList)
      } else {
        errorCode = tryResult.getErrorCode()
        break
      }
    }
    return if (errorCode == 0) ok(TryCostResultSet(resultList, logSource)) else err(errorCode)
  }

  fun execCost(self: Self, tryResultSet: TryCostResultSet): CostResultSet {
    val logSource = tryResultSet.logSource
    val results = tryResultSet.results.map {
      val processor = processors[it.cost.type] ?: error("should not happen.")
      processor.execCost(self, it, logSource)
    }
    log(self, results, logSource)
    return CostResultSet(results)
  }

  fun tryAndExecCost(self: Self, costs: Collection<Cost>, logSource: Int): Result2<CostResultSet> {
    if (costs.isEmpty()) return ok(CostResultSet(emptyList()))
    return tryCost(self, costs, logSource).map { execCost(self, it) }
  }

  private fun log(self: Self, results: List<RewardOrCostResult>, logSource: Int) {
    // TODO
  }
}
