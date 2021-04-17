package play.example.game.module.reward.model

import play.example.game.module.reward.config.CurrencyRawReward
import play.example.game.module.reward.config.ItemRawReward
import play.example.game.module.reward.config.NonRawReward
import play.example.game.module.reward.config.RawReward
import play.util.enumration.IdEnum
import play.util.enumration.IdEnumOps
import play.util.enumration.idEnumOpsOf

enum class RewardType(
  override val id: Int,
  val rawRewardClass: Class<out RawReward> = CurrencyRawReward::class.java,
  val rewardClass: Class<out Reward> = CurrencyReward::class.java
) : IdEnum<RewardType> {

  None(0, NonRawReward::class.java, NonReward::class.java),
  Item(1, ItemRawReward::class.java, ItemReward::class.java),
  Gold(2),
  ;

  companion object : IdEnumOps<RewardType> by idEnumOpsOf(values())
}
