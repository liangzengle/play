package play.example.module.reward.config

import play.config.validation.ReferTo
import play.example.module.item.config.ItemConfig
import play.example.module.player.args.PlayerArgs
import play.example.module.reward.model.*
import javax.validation.constraints.Min

abstract class RawReward(type: RewardType, num: String) {
  private val count = try {
    val value = num.toInt()
    require(value >= 0) { "num >= 0" }
    value
  } catch (e: NumberFormatException) {
    -1
  }

  init {
    require(type.rawRewardClass == javaClass) { "奖励类型不匹配${javaClass.simpleName}${type}" }
  }

  abstract val type: RewardType
  protected abstract val num: String

  fun getCount(args: Map<String, Any>): Int = if (count >= 0) count else TODO("eval")

  abstract fun toReward(args: PlayerArgs): Reward
}

object NonRawReward : RawReward(RewardType.None, "0") {
  override val type: RewardType = RewardType.None
  override val num: String = "0"

  override fun toReward(args: PlayerArgs): NonReward = NonReward
}

data class RawCurrencyReward(override val type: RewardType, override val num: String) : RawReward(type, num) {
  override fun toReward(args: PlayerArgs): CurrencyReward = CurrencyReward(type, getCount(args))
}

abstract class RawItemLikeReward(type: RewardType, num: String) : RawReward(type, num) {
  abstract val cfgId: Int
}

class RawItemReward(
  @field:Min(1)
  @ReferTo(ItemConfig::class)
  override val cfgId: Int,
  override val num: String
) : RawItemLikeReward(RewardType.Item, num) {

  override val type: RewardType = RewardType.Item

  override fun toReward(args: PlayerArgs): ItemReward = ItemReward(cfgId, getCount(args))
}
