package play.example.game.app.module.reward.config

import play.example.game.app.module.item.config.ItemResource
import play.example.game.app.module.reward.json.RawRewardTypeResolver
import play.example.game.app.module.reward.model.*
import play.res.validation.ReferTo
import play.util.el.Eval
import play.util.json.JsonAbstractType
import javax.validation.constraints.Min

@JsonAbstractType(RawRewardTypeResolver::class)
abstract class RawReward(type: RewardType, num: String) {
  private val count = try {
    val value = num.toInt()
    require(value >= 0) { "num >= 0" }
    value
  } catch (e: NumberFormatException) {
    -1
  }

  init {
    require(type.rawRewardClass == javaClass) { "奖励类型不匹配${javaClass.simpleName}$type" }
  }

  abstract val type: RewardType
  protected abstract val num: String

  fun getCount(args: Map<String, Any>): Int = if (count >= 0) count else Eval.eval(num, args).getIntOrThrow()

  abstract fun toReward(args: Map<String, Any>): Reward
}

object NonRawReward : RawReward(RewardType.None, "0") {
  override val type: RewardType = RewardType.None
  override val num: String = "0"

  override fun toReward(args: Map<String, Any>): NonReward = NonReward

  override fun toString(): String = this.javaClass.simpleName
}

data class CurrencyRawReward(override val type: RewardType, override val num: String) : RawReward(type, num) {
  override fun toReward(args: Map<String, Any>): CurrencyReward = CurrencyReward(type, getCount(args))
}

abstract class ItemLikeRawReward(type: RewardType, num: String) : RawReward(type, num) {
  abstract val cfgId: Int
}

data class ItemRawReward(
  @field:Min(1)
  @ReferTo(ItemResource::class)
  override val cfgId: Int,
  override val num: String
) : ItemLikeRawReward(RewardType.Item, num) {

  override val type: RewardType = RewardType.Item

  override fun toReward(args: Map<String, Any>): ItemReward = ItemReward(cfgId, getCount(args))
}
