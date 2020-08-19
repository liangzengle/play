package play.example.module.reward.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.auto.service.AutoService
import play.Log
import play.example.module.reward.model.NonReward
import play.example.module.reward.model.Reward
import play.example.module.reward.model.RewardType

/**
 * Reward的反序列化
 * @author LiangZengle
 */
@AutoService(com.fasterxml.jackson.databind.Module::class)
class RewardJacksonModule : SimpleModule() {

  override fun getModuleName(): String = "Reward"

  init {
    addDeserializer(Reward::class.java, RewardJacksonDeserializer())
    addDeserializer(RawReward::class.java, RawRewardJacksonDeserializer())
  }
}

private class RewardJacksonDeserializer : StdDeserializer<Reward>(Reward::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Reward {
    val node = p.codec.readTree<JsonNode>(p)
    val type = node.get("type")?.asInt() ?: RewardType.None.id
    val rewardType = RewardType.getOrDefault(type, RewardType.None)
    if (rewardType == RewardType.None) {
      Log.error { "错误的奖励类型(Reward): $node" }
      return NonReward
    }
    val clazz = rewardType.rewardClass
    return p.codec.treeToValue(node, clazz)
  }
}

private class RawRewardJacksonDeserializer : StdDeserializer<RawReward>(RawReward::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): RawReward {
    val node = p.codec.readTree<JsonNode>(p)
    val type = node.get("type")?.asInt() ?: RewardType.None.id
    val rewardType = RewardType.getOrDefault(type, RewardType.None)
    if (rewardType == RewardType.None) {
      Log.error { "错误的奖励类型(RawReward): $node" }
      return NonRawReward
    }
    val clazz = rewardType.rawRewardClass
    return p.codec.treeToValue(node, clazz)
  }
}
