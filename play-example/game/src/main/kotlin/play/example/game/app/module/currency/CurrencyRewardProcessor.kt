package play.example.game.app.module.currency

import org.springframework.stereotype.Component
import play.example.game.app.module.currency.domain.CurrencyTypes
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.item.res.ItemResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.reward.RewardProcessor
import play.example.game.app.module.reward.model.*
import play.util.control.Result2
import play.util.control.ok

@Component
class CurrencyRewardProcessor(private val currencyService: PlayerCurrencyService) :
  RewardProcessor {

  override fun support(reward: Reward): Boolean {
    return ItemResourceSet.getOrNull(reward.id)?.type == ItemType.Currency
  }

  override fun tryReward(
    self: Self,
    reward: Reward,
    logSource: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult> {
    if (reward.num < 1) return ok(TryRewardResult(reward, 0, 0, 0))
    val fcmValue = 0L // TODO
    return ok(TryRewardResult(reward, 0, fcmValue, 0))
  }

  override fun execReward(self: Self, tryRewardResult: TryRewardResult, logSource: Int): RewardResult {
    val currencyType = CurrencyTypes.getOrThrow(tryRewardResult.reward.id)
    if (tryRewardResult.rewardCount < 1) {
      return RewardResult(tryRewardResult, currencyService.getCurrency(self, currencyType))
    }
    val newValue = currencyService.addCurrency(self, currencyType, tryRewardResult.rewardCount)
    return RewardResult(tryRewardResult, newValue)
  }

  override fun tryCost(self: Self, cost: Cost, logSource: Int): Result2<TryCostResultSetLike> {
    if (cost.num < 1) {
      return ok()
    }
    val currencyType = CurrencyTypes.getOrThrow(cost.id)
    return if (currencyService.getCurrency(self, currencyType) < cost.num) {
      currencyType.noEnoughErrorCode
    } else {
      ok(TryCostResultSet(TryCostResult(cost.reward).asList(), logSource))
    }
  }

  override fun execCost(self: Self, tryCostResult: TryCostResult, logSource: Int): CostResult {
    val currencyType = CurrencyTypes.getOrThrow(tryCostResult.cost.id)
    val newValue = currencyService.reduceCurrency(self, currencyType, tryCostResult.costCount)
    return CostResult(tryCostResult, newValue)
  }
}
