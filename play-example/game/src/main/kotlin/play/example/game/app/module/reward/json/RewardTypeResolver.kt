package play.example.game.app.module.reward.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.model.RewardType
import play.example.game.app.module.reward.res.RawReward
import play.util.json.AbstractTypeResolver

class RawRewardTypeResolver : AbstractTypeResolver<RawReward>() {
  override fun resolve(node: JsonNode): Class<out RawReward> {
    return getRewardType(node).rawRewardClass
  }

  override fun recover(ex: Throwable): RawReward? {
//    return ModeDependent.logAndRecover(ex, NonRawReward) { ex.message }
    return null
  }
}


class RewardTypeResolver : AbstractTypeResolver<Reward>() {
  override fun resolve(node: JsonNode): Class<out Reward> {
    return getRewardType(node).rewardClass
  }

  override fun recover(ex: Throwable): Reward? {
//    return ModeDependent.logAndRecover(ex, NonReward) { ex.message }
    return null
  }
}

private fun getRewardType(node: JsonNode): RewardType {
  val typeNode = node.get("type")
  val rewardType = if (typeNode.isInt) {
    RewardType.getOrThrow(typeNode.intValue())
  } else {
    RewardType.valueOf(typeNode.textValue())
  }
  if (rewardType == RewardType.None) {
    throw IllegalStateException("错误的奖励类型(Reward): $node")
  }
  return rewardType
}
