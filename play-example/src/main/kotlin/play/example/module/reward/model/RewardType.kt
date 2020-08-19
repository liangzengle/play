package play.example.module.reward.model

import play.example.common.IdEnum
import play.example.common.IdEnumCompanion
import play.example.module.reward.config.RawCurrencyReward
import play.example.module.reward.config.RawItemReward
import play.example.module.reward.config.RawReward

enum class RewardType(
  override val id: Int,
  val rawRewardClass: Class<out RawReward> = RawCurrencyReward::class.java,
  val rewardClass: Class<out Reward> = CurrencyReward::class.java
) : IdEnum<RewardType> {

  None(0, RawReward::class.java, Reward::class.java),
  Item(1, RawItemReward::class.java, ItemReward::class.java),
  Gold(2),
  ;

  companion object : IdEnumCompanion<RewardType> {
    override val elems: Array<RewardType> = values()
  }
}
