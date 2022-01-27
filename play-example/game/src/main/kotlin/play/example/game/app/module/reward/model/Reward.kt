package play.example.game.app.module.reward.model

import jakarta.validation.constraints.Min
import play.example.game.app.module.item.res.ItemResource
import play.example.game.app.module.reward.json.RewardTypeResolver
import play.res.validation.constraints.ReferTo
import play.util.json.Json
import play.util.json.JsonAbstractType

@JsonAbstractType(RewardTypeResolver::class)
abstract class Reward(@JvmField val type: RewardType, @JvmField @field:Min(0) val num: Long) {
  init {
    require(num >= 0) { "num($num) >= 0" }
    require(javaClass == type.rewardClass) { "奖励类型不匹配${javaClass.simpleName}$type" }
  }

  fun toRewardList(): RewardList = RewardList(this)

  operator fun plus(count: Long) = copy(Math.addExact(num, count))

  operator fun minus(count: Long) = copy(Math.subtractExact(num, count))

  operator fun times(count: Long) = copy(Math.multiplyExact(num, count))

  operator fun times(count: Double) = copy((num * count).toLong())

  operator fun div(count: Long) = copy(Math.addExact(num, count))

  operator fun div(count: Double) = copy((num / count).toLong())

  abstract fun copy(num: Long = this.num): Reward

  abstract fun canMerge(other: Reward, isCost: Boolean = false): Boolean

  override fun toString(): String {
    return Json.stringify(this)
  }
}

object NonReward : Reward(RewardType.None, 0) {
  override fun copy(num: Long): Reward {
    return this
  }

  override fun canMerge(other: Reward, isCost: Boolean): Boolean = false
}

abstract class ItemLikeReward(type: RewardType, num: Long) : Reward(type, num) {
  abstract val cfgId: Int

  override fun canMerge(other: Reward, isCost: Boolean): Boolean {
    return type == other.type && other is ItemLikeReward && other.cfgId == cfgId
  }
}

class CurrencyReward(type: RewardType, num: Long) : Reward(type, num) {
  override fun copy(num: Long): CurrencyReward = CurrencyReward(type, num)
  override fun canMerge(other: Reward, isCost: Boolean): Boolean = type == other.type
}

class ItemReward(
  @field:Min(1) @ReferTo(ItemResource::class) override val cfgId: Int,
  num: Long
) : ItemLikeReward(RewardType.Item, num) {
  override fun copy(num: Long): ItemReward = ItemReward(cfgId, num)
}
