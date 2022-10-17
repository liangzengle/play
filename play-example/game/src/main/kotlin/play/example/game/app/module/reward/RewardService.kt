package play.example.game.app.module.reward

import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import org.springframework.stereotype.Component
import play.Orders
import play.example.common.StatusCode
import play.example.game.app.module.common.res.CommonSettingConf
import play.example.game.app.module.mail.entity.Mail
import play.example.game.app.module.mail.event.PlayerMailEvent
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.reward.exception.RewardProcessorNotFoundException
import play.example.game.app.module.reward.model.*
import play.example.game.app.module.reward.model.TransformedResult.*
import play.util.control.Result2
import play.util.control.err
import play.util.control.map
import play.util.control.ok
import play.util.logging.getLogger

@Component
class RewardService(
  private val playerEventBus: PlayerEventBus,
  processorList: List<RewardProcessor>
) {
  private val logger = getLogger()

  private val sortedProcessors = processorList.sortedWith(Orders.comparator)

  private fun getProcessor(reward: Reward): RewardProcessor? {
    for (i in sortedProcessors.indices) {
      val processor = sortedProcessors[i]
      if (processor.support(reward)) {
        return processor
      }
    }
    return null
  }

  fun tryReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    return tryReward(self, reward.toRewardList(), logSource, bagFullStrategy, checkFcm)
  }

  fun tryReward(
    self: Self,
    rewards: RewardList,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<TryRewardResultSet> {
    val rewardList = transform(self, rewards.asList())
    var errorCode = 0
    var usedBagSize = 0
    val resultList = ArrayList<TryRewardResult>(rewardList.size)
    for (i in rewardList.indices) {
      val reward = rewardList[i]
      if (reward.num < 1) {
        continue
      }
      val processor = getProcessor(reward)
      if (processor == null) {
        errorCode = StatusCode.Failure.getErrorCode()
        logger.error(RewardProcessorNotFoundException(reward)) { "奖励预判异常: $reward" }
        continue
      }
      val tryResult = processor.tryReward(self, reward, logSource, usedBagSize, bagFullStrategy, checkFcm)
      if (tryResult.isErr()) {
        errorCode = tryResult.getErrorCode()
        break
      } else {
        val result = tryResult.get()
        usedBagSize += result.usedBagSize
        if (result.rewardCount > 0 || result.mailCount > 0) {
          resultList += result
        }
      }
    }
    return if (errorCode == 0) ok(TryRewardResultSet(resultList, bagFullStrategy, logSource)) else err(errorCode)
  }

  fun execReward(self: Self, tryResultSet: TryRewardResultSet): RewardResultSet {
    if (tryResultSet.results.isEmpty()) {
      return RewardResultSet(emptyList())
    }
    val logSource = tryResultSet.logSource
    val results = ArrayList<RewardResult>(tryResultSet.results.size)
    for (tryRewardResult in tryResultSet.results) {
      try {
        val rewardResult = getProcessor(tryRewardResult.reward)!!.execReward(self, tryRewardResult, logSource)
        results.add(rewardResult)
      } catch (e: Exception) {
        logger.error(e) { "self=$self, reward=${tryRewardResult.reward}, logSource=$logSource" }
      }
    }
    if (tryResultSet.bagFullStrategy == BagFullStrategy.Mail) {
      val mailRewards = results.mapNotNull { it.mailReward }
      if (mailRewards.isNotEmpty()) {
        val mail = Mail {
          title(CommonSettingConf.bagFullMailTitleId)
          content(CommonSettingConf.bagFullMailContentId)
          rewards(mailRewards, logSource)
        }
        playerEventBus.publish(PlayerMailEvent(self.id, mail))
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
    rewards: RewardList,
    logSource: Int,
    bagFullStrategy: BagFullStrategy = BagFullStrategy.Mail,
    checkFcm: Boolean = true
  ): Result2<RewardResultSet> {
    return tryReward(self, rewards, logSource, bagFullStrategy, checkFcm).map { execReward(self, it) }
  }

  fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSet> {
    return tryCost(self, cost.toCostList(), logSource)
  }

  fun tryCost(self: Self, costs: CostList, logSource: Int): Result2<TryCostResultSet> {
    if (costs.isEmpty()) {
      return ok(TryCostResultSet(emptyList(), logSource))
    }
    val costList = costs.asList()
    val resultList = ArrayList<TryCostResult>(costList.size)
    var errorCode = 0
    for (i in costList.indices) {
      val cost = costList[i]
      if (cost.num < 1) {
        continue
      }
      val processor = getProcessor(cost.reward)
      if (processor == null) {
        errorCode = StatusCode.Failure.getErrorCode()
        logger.error(RewardProcessorNotFoundException(cost.reward)) { "消耗预判失败: $cost" }
        break
      }
      val tryResult = processor.tryCost(self, cost, logSource)
      if (tryResult.isOk()) {
        tryResult.get().appendTo(resultList)
      } else {
        errorCode = tryResult.getErrorCode()
        break
      }
    }
    return if (errorCode == 0) ok(TryCostResultSet(resultList, logSource)) else err(errorCode)
  }

  fun execCost(self: Self, tryResultSet: TryCostResultSet): CostResultSet {
    if (tryResultSet.isEmpty()) {
      return CostResultSet(emptyList())
    }
    val logSource = tryResultSet.logSource
    val results = ArrayList<CostResult>(tryResultSet.results.size)
    for (tryCostResult in tryResultSet.results) {
      try {
        val costResult = getProcessor(tryCostResult.cost)!!.execCost(self, tryCostResult, logSource)
        results.add(costResult)
      } catch (e: Exception) {
        logger.error(e) { "self=$self, cost=${tryCostResult.cost}, logSource=$logSource" }
      }
    }
    log(self, results, logSource)
    return CostResultSet(results)
  }

  fun tryAndExecCost(self: Self, costs: CostList, logSource: Int): Result2<CostResultSet> {
    if (costs.isEmpty()) return ok(CostResultSet(emptyList()))
    return tryCost(self, costs, logSource).map { execCost(self, it) }
  }

  private fun transform(self: Self, merged: List<Reward>): List<Reward> {
    val indexToTransformed = IntObjectMaps.mutable.empty<TransformedResult>()
    for (i in merged.indices) {
      val reward = merged[i]
      val processor = getProcessor(reward) ?: continue
      val transformed = processor.transform(self, reward)
      if (transformed != Unchanged) {
        indexToTransformed.put(i, transformed)
      }
    }
    if (indexToTransformed.isEmpty) {
      return merged
    }
    val result = ArrayList<Reward>(merged.size)
    for (i in merged.indices) {
      val transformed = indexToTransformed.get(i)
      if (transformed == null) {
        result.add(merged[i])
        continue
      }
      when (transformed) {
        is Single -> result.add(transformed.reward)
        is None -> continue
        is Multi -> result.addAll(transformed.rewardList)
        is Unchanged -> result.add(merged[i])
      }
    }
    return RewardHelper.mergeReward(result)
  }

  private fun log(self: Self, results: List<RewardOrCostResult>, logSource: Int) {
    // TODO
  }
}
