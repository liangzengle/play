package play.example.game.app.module.activity.base.entity

import play.entity.IntIdEntity
import play.entity.cache.CacheSpec
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.util.collection.SerializableAttributeMap
import play.util.max

/**
 *
 * @author LiangZengle
 */
@CacheSpec(expireEvaluator = ActivityEntityExpireEvaluator::class)
class ActivityEntity(id: Int) : IntIdEntity(id) {

  var stage = ActivityStage.None

  var startTime = 0L

  var endTime = 0L

  var closeTime = 0L

  var openTimes = 0L

  var suspendTime = 0L

  var suspendedMillis = 0L

  val data = SerializableAttributeMap()

  fun suspend(startTime: Long) {
    suspendTime = startTime
  }

  fun resume(resumeTime: Long) {
    suspendedMillis += (resumeTime - suspendTime) max 0
    suspendTime = 0
  }

  fun isSuspended(): Boolean {
    return suspendTime > 0
  }
}
