package play.example.module.reward.model

import play.example.module.reward.config.RawCurrencyReward
import play.example.module.reward.config.RawItemReward
import play.example.module.reward.config.RawReward
import play.util.enumration.IdEnum
import play.util.enumration.IdEnumOps
import play.util.enumration.idEnumOpsOf

enum class RewardType(
  override val id: Int,
  val rawRewardClass: Class<out RawReward> = RawCurrencyReward::class.java,
  val rewardClass: Class<out Reward> = CurrencyReward::class.java
) : IdEnum<RewardType> {

  None(0, RawReward::class.java, Reward::class.java),
  Item(1, RawItemReward::class.java, ItemReward::class.java),
  Gold(2),
  ;

  companion object : IdEnumOps<RewardType> by idEnumOpsOf(values())
}
