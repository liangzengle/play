package play.example.game.app.module.mail.entity

import play.db.Merge
import play.entity.LongIdEntity
import play.entity.cache.CacheSpec
import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.player.condition.PlayerCondition
import play.example.game.app.module.reward.res.RawReward
import play.util.collection.ConcurrentHashSetLong

/**
 * 公共邮件
 *
 * @param id 邮件id
 * @param title 标题
 * @param content 内容
 * @param receiveConditions 接收条件
 * @param rewards 奖励
 * @param logSource 来源
 * @param startTime 生效起始时间
 * @param expireTime 过期时间
 * @param createTime 创建时间
 */
@Merge(Merge.Strategy.Clear)
@CacheSpec(loadAllOnInit = true, neverExpire = true)
class PublicMailEntity(
  id: Long,
  val title: I18nText,
  val content: I18nText,
  val receiveConditions: List<PlayerCondition>,
  val rewards: List<RawReward>,
  val logSource: Int,
  val startTime: Long,
  val expireTime: Long,
  val createTime: Long
) : LongIdEntity(id) {

  val received = ConcurrentHashSetLong()

  fun isReceived(playerId: Long) = received.contains(playerId)

  fun addReceiver(playerId: Long) = received.add(playerId)
}
