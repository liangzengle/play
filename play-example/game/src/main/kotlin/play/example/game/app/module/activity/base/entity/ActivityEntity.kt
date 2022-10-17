package play.example.game.app.module.activity.base.entity

import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.example.game.app.module.activity.base.ActivityDataKey
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.util.collection.ConcurrentHashSetLong
import play.util.collection.SerializableAttributeMap

/**
 *
 * @author LiangZengle
 */
@CacheSpec(expireEvaluator = ActivityEntityExpireEvaluator::class)
class ActivityEntity(id: Int) : IntIdEntity(id) {

  /** 当前阶段 */
  var stage = ActivityStage.None

  /** 活动开始时间 */
  var startTime = 0L

  /** 活动结束时间 */
  var endTime = 0L

  /** 活动关闭时间 */
  var closeTime = 0L

  /** 活动已开启次数 */
  var openTimes = 0L

  /** 活动暂停时间 */
  var suspendTime = 0L

  /** 活动暂停时长 */
  var suspendedMillis = 0L

  /** 活动数据 */
  var data = SerializableAttributeMap()
    private set

  fun isSuspended(): Boolean {
    return suspendTime > 0
  }

  fun clearData() {
    data = SerializableAttributeMap()
  }

  fun join(playerId: Long): Boolean {
    return data.attr(ActivityDataKey.JoinedPlayers).computeIfAbsent { ConcurrentHashSetLong() }.add(playerId)
  }
}
