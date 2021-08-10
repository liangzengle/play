package play.example.game.app.module.reward.processor

import play.example.common.StatusCode
import play.example.game.app.module.item.config.ItemResourceSet
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.player.Self
import play.example.game.app.module.reward.model.*
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Named
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
@Named
class ItemRewardProcessor : RewardProcessor<ItemReward>(RewardType.Item) {

  override fun needTransform(self: Self, reward: ItemReward): Boolean {
    val itemCfg = ItemResourceSet.getOrNull(reward.cfgId) ?: return false
    if (itemCfg.type == ItemType.Currency) {
      return true
    }
    return super.needTransform(self, reward)
  }

  override fun transform(self: Self, reward: ItemReward): List<Reward> {
    val itemCfg = ItemResourceSet.getOrThrow(reward.cfgId)
    if (itemCfg.type == ItemType.Currency) {
      return listOf(CurrencyReward(RewardType.getOrThrow(itemCfg.subtype), reward.num))
    }
    return super.transform(self, reward)
  }

  override fun tryReward(
    self: Self,
    reward: ItemReward,
    logSource: Int,
    usedBagSize: Int,
    bagFullStrategy: BagFullStrategy,
    checkFcm: Boolean
  ): Result2<TryRewardResult> {
    // TODO
    val itemCfg = ItemResourceSet.getOrNull(reward.cfgId) ?: return StatusCode.ConfigNotFound
    return ok(TryRewardResult(reward, usedBagSize, 0, 0))
  }

  override fun execReward(self: Self, tryRewardResult: TryRewardResult, logSource: Int): ItemLikeRewardResult {
    return ItemLikeRewardResult(tryRewardResult)
  }

  override fun tryCost(self: Self, cost: ItemReward, logSource: Int): Result2<TryCostResultSetLike> {
    return StatusCode.Failure
  }

  override fun execCost(self: Self, tryCostResult: TryCostResult, logSource: Int): ItemLikeCostResult {
    return ItemLikeCostResult(tryCostResult)
  }
}
