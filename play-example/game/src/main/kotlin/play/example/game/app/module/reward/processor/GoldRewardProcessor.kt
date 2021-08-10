package play.example.game.app.module.reward.processor

import play.example.game.app.module.player.Self
import play.example.game.app.module.reward.model.RewardType
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Named
class GoldRewardProcessor : CurrencyRewardProcessor(RewardType.Gold) {
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
}
