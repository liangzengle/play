package play.example.module.reward.processor

import play.example.module.player.Self
import play.example.module.reward.model.*
import play.util.control.Result2
import play.util.control.err
import play.util.control.ok
import play.util.max

abstract class CurrencyRewardProcessor : RewardProcessor<CurrencyReward>() {

  override fun tryReward(
    self: Self,
    reward: CurrencyReward,
    source: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult> {
    if (reward.num < 1) return ok(TryRewardResult(reward, 0, 0, 0))
    val additionValue = addition(self, reward.num, source) max 0
    val fcmValue = 0 // TODO
    return ok(TryRewardResult(reward, 0, additionValue + fcmValue, 0))
  }

  override fun execReward(self: Self, tryRewardResult: TryRewardResult, source: Int): CurrencyRewardResult {
    if (tryRewardResult.rewardCount < 1) {
      return CurrencyRewardResult(tryRewardResult, getValue(self))
    }
    val newValue = addValue(self, tryRewardResult.rewardCount)
    return CurrencyRewardResult(tryRewardResult, newValue)
  }

  override fun tryCost(self: Self, cost: CurrencyReward, source: Int): Result2<TryCostResultSetLike> {
    if (cost.num < 1) {
      return ok()
    }
    return if (getValue(self) < cost.num) {
      err(notEnoughErrorCode())
    } else {
      ok(TryCostResultSet(TryCostResult(cost).asList(), source))
    }
  }

  override fun execCost(self: Self, tryCostResult: TryCostResult, source: Int): CurrencyCostResult {
    val newValue = reduceValue(self, tryCostResult.costCount)
    return CurrencyCostResult(tryCostResult, newValue)
  }

  protected open fun addition(self: Self, original: Int, source: Int): Int = 0
  protected abstract fun getValue(self: Self): Long
  protected abstract fun addValue(self: Self, add: Int): Long
  protected abstract fun reduceValue(self: Self, reduce: Int): Long
  abstract fun notEnoughErrorCode(): Int
}
