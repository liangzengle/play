package play.example.game.app.module.reward.message

import play.example.game.app.module.reward.model.RewardResult
import play.example.game.app.module.reward.model.RewardResultSet
import play.example.reward.message.RewardResultProto
import play.example.reward.message.RewardResultSetProto

/**
 *
 * @author LiangZengle
 */
fun RewardResultSet.toProto(): RewardResultSetProto {
  return RewardResultSetProto(results.map { it.toProto() })
}

fun RewardResult.toProto(): RewardResultProto {
  return RewardResultProto(this.reward.id, this.actualCount, this.currentValue)
}
