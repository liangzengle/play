package play.example.module.reward.model

import play.config.validation.ReferTo
import play.example.module.item.config.ItemConfig
import javax.validation.constraints.Min

abstract class Reward(type: RewardType, num: Int) {
  init {
    require(num >= 0) { "num($num) >= 0" }
    require(javaClass == type.rewardClass) { "奖励类型不匹配${javaClass.simpleName}${type}" }
  }

  abstract val type: RewardType

  abstract val num: Int

  operator fun plus(count: Int) = copy(Math.addExact(num, count))

  operator fun minus(count: Int) = copy(Math.subtractExact(num, count))

  operator fun times(count: Int) = copy(Math.multiplyExact(num, count))

  operator fun times(count: Double) = copy((num * count).toInt())

  operator fun div(count: Int) = copy(Math.addExact(num, count))

  operator fun div(count: Double) = copy((num / count).toInt())

  abstract fun copy(num: Int = this.num): Reward

  abstract fun canMerge(other: Reward, isCost: Boolean = false): Boolean
}

object NonReward : Reward(RewardType.None, 0) {
  override val type: RewardType
    get() = RewardType.None
  override val num: Int
    get() = 0

  override fun copy(num: Int): Reward {
    // TODO warning
    return this
  }

  override fun canMerge(other: Reward, isCost: Boolean): Boolean = false
}

abstract class ItemLikeReward(type: RewardType, num: Int) : Reward(type, num) {
  abstract val cfgId: Int

  override fun canMerge(other: Reward, isCost: Boolean): Boolean {
    return type == other.type && other is ItemLikeReward && other.cfgId == cfgId
  }
}

data class CurrencyReward(override val type: RewardType, @field:Min(0) override val num: Int) : Reward(type, num) {
  override fun copy(num: Int): CurrencyReward = CurrencyReward(type, num)
  override fun canMerge(other: Reward, isCost: Boolean): Boolean = type == other.type
}

data class ItemReward(
  @field:Min(1) @ReferTo(ItemConfig::class) override val cfgId: Int,
  @field:Min(0) override val num: Int
) :
  ItemLikeReward(RewardType.Item, num) {
  override val type: RewardType get() = RewardType.Item
  override fun copy(num: Int): ItemReward = ItemReward(cfgId, num)
}
