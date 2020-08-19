package play.example.game.app.module.mail.entity

import play.db.Merge
import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.entity.cache.NeverExpireEvaluator
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
 * @param received 已接收的玩家
 * @param rewards 奖励
 * @param logSource 来源
 * @param startTime 生效起始时间
 * @param endTime 过期时间
 * @param createTime 创建时间
 */
@Merge(Merge.Strategy.PreservePrimary)
@CacheSpec(loadAllOnInit = true, neverExpire = true)
class PublicMail(
  id: Int,
  val title: String,
  val content: String,
  val receiveConditions: List<PlayerCondition>,
  val received: ConcurrentHashSetLong,
  val rewards: List<RawReward>,
  val logSource: Int,
  val startTime: Long,
  val endTime: Long,
  val createTime: Long
) : IntIdEntity(id) {

  fun isReceived(playerId: Long) = received.contains(playerId)

  fun addReceiver(playerId: Long) = received.add(playerId)
}
