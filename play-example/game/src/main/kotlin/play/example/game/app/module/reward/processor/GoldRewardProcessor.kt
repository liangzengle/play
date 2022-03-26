package play.example.game.app.module.reward.processor

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.reward.model.RewardType

@Component
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
