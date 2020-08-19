package play.example.module.reward.processor

import play.example.module.player.Self
import play.example.module.reward.model.RewardType
import javax.inject.Singleton

@Singleton
class GoldRewardProcessor : CurrencyRewardProcessor() {
  override fun getValue(self: Self): Long {
    TODO("Not yet implemented")
  }

  override fun addValue(self: Self, add: Int): Long {
    TODO("Not yet implemented")
  }

  override fun reduceValue(self: Self, reduce: Int): Long {
    TODO("Not yet implemented")
  }

  override fun notEnoughErrorCode(): Int {
    TODO("Not yet implemented")
  }

  override val rewardType: RewardType = RewardType.Gold

}
