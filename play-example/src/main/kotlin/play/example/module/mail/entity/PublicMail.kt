package play.example.module.mail.entity

import play.db.EntityInt
import play.db.Merge
import play.db.cache.CacheSpec
import play.db.cache.NeverExpireEvaluator
import play.example.common.collection.NonBlockingHashSetLong
import play.example.module.mail.domain.ReceiverQualification
import play.example.module.reward.config.RawReward

/**
 * 公共邮件
 *
 * @param id 邮件id
 * @param title 标题
 * @param content 内容
 * @param qualification 接收条件
 * @param received 已接收的玩家
 * @param rewards 奖励
 * @param source 来源
 * @param startTime 生效起始时间
 * @param endTime 过期时间
 * @param createTime 创建时间
 */
@Merge(Merge.Strategy.PRESERVE_PRIMARY)
@CacheSpec(initialSize = CacheSpec.SIZE_ONE, loadAllOnInit = true, expireEvaluator = NeverExpireEvaluator::class)
class PublicMail(
  id: Int,
  val title: String,
  val content: String,
  val qualification: ReceiverQualification,
  val received: NonBlockingHashSetLong,
  val rewards: List<RawReward>,
  val source: Int,
  val startTime: Long,
  val endTime: Long,
  val createTime: Long
) : EntityInt(id) {

  fun isReceived(playerId: Long) = received.contains(playerId)

  fun addReceiver(playerId: Long) = received.add(playerId)
}
