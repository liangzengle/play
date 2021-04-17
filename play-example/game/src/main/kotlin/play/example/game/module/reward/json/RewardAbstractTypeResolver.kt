package play.example.game.module.reward.json

import com.fasterxml.jackson.databind.node.ObjectNode
import play.Log
import play.ModeDependent
import play.example.game.module.reward.config.NonRawReward
import play.example.game.module.reward.config.RawReward
import play.example.game.module.reward.model.NonReward
import play.example.game.module.reward.model.Reward
import play.example.game.module.reward.model.RewardType
import play.util.json.AbstractTypeResolver

class RawRewardAbstractTypeResolver : AbstractTypeResolver<RawReward>() {
  override fun resolve(node: ObjectNode): Class<out RawReward> {
    return getRewardType(node).rawRewardClass
  }

  override fun recover(ex: Throwable): RawReward {
    return ModeDependent.logAndRecover(ex, NonRawReward) { ex.message }
  }
}


class RewardAbstractTypeResolver : AbstractTypeResolver<Reward>() {
  override fun resolve(node: ObjectNode): Class<out Reward> {
    return getRewardType(node).rewardClass
  }

  override fun recover(ex: Throwable): Reward {
    return ModeDependent.logAndRecover(ex, NonReward) { ex.message }
  }
}

private fun getRewardType(node: ObjectNode): RewardType {
  val typeNode = node.get("type")
  val rewardType = if (typeNode.isInt) {
    RewardType.getOrThrow(typeNode.intValue())
  } else {
    RewardType.valueOf(typeNode.textValue())
  }
  if (rewardType == RewardType.None) {
    Log.error { "错误的奖励类型(Reward): $node" }
  }
  return rewardType
}
