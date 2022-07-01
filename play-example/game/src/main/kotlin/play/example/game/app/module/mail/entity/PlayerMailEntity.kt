package play.example.game.app.module.mail.entity

import play.entity.cache.CacheIndex
import play.entity.cache.InitialCacheSize
import play.example.game.app.module.player.entity.AbstractPlayerEntity
import play.example.game.app.module.reward.model.Reward

@InitialCacheSize("x100")
class PlayerMailEntity(
  id: Long,
  @CacheIndex override val playerId: Long,
  val title: String,
  val content: String,
  val rewards: List<Reward>,
  val logSource: Int,
  var status: Int,
  val createTime: Long
) : AbstractPlayerEntity(id) {

  fun setRead() {
    status = status or 1
  }

  fun isRead() = (status and 1) != 0

  fun setRewarded() {
    status = status or 2
  }

  fun isRewarded() = (status and 2) != 0

  fun hasReward() = !isRewarded() && rewards.isNotEmpty()

  override fun toString(): String {
    return "PlayerMailEntity(title='$title', content='$content', rewards=$rewards, logSource=$logSource, status=$status, createTime=$createTime)"
  }
}
