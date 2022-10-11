package play.example.game.app.module.mail.entity

import play.entity.cache.CacheIndex
import play.entity.cache.InitialCacheSize
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.player.entity.AbstractPlayerEntity
import play.example.game.app.module.reward.model.RewardList
import play.util.primitive.Bit

@InitialCacheSize("x100")
class PlayerMailEntity(
  id: Long,
  @CacheIndex
  override val playerId: Long,
  val title: I18nText,
  val content: I18nText,
  val rewards: RewardList,
  val logSource: Int,
  var status: Int,
  val createTime: Long,
  val displayTime: Long
) : AbstractPlayerEntity(id) {

  fun setRead() {
    status = Bit.set1(status, 1)
  }

  fun isRead() = Bit.is1(status, 1)

  fun setRewarded() {
    status = Bit.set1(status, 2)
  }

  fun isRewarded() = Bit.is1(status, 2)

  fun hasReward() = !isRewarded() && rewards.isNotEmpty()

  override fun toString(): String {
    return "PlayerMailEntity(title='$title', content='$content', rewards=$rewards, logSource=$logSource, status=$status, createTime=$createTime)"
  }
}
