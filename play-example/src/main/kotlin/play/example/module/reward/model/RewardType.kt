package play.example.module.reward.model

import play.EagerlyLoad
import play.example.common.RuntimeEnum
import play.example.common.RuntimeEnumFactoryOps
import play.example.module.reward.config.CurrencyRawReward
import play.example.module.reward.config.ItemRawReward
import play.example.module.reward.config.NonRawReward
import play.example.module.reward.config.RawReward

@EagerlyLoad
class RewardType(
  id: Int,
  name: String,
  val rawRewardClass: Class<out RawReward> = CurrencyRawReward::class.java,
  val rewardClass: Class<out Reward> = CurrencyReward::class.java
) : RuntimeEnum(id, name) {

  companion object : RuntimeEnumFactoryOps<RewardType> by Factory(RewardType::class.java) {
    @JvmStatic
    val None = RewardType(0, "空奖励", NonRawReward::class.java, NonReward::class.java)

    @JvmStatic
    val Gold = RewardType(1, "货币", CurrencyRawReward::class.java, CurrencyReward::class.java)

    @JvmStatic
    val Item = RewardType(2, "物品", ItemRawReward::class.java, ItemReward::class.java)
  }
}
