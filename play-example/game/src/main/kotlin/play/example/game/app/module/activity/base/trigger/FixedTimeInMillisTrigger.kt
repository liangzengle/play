package play.example.game.app.module.activity.base.trigger

import play.util.time.Time
import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
class FixedTimeInMillisTrigger(private val timeMillis: Long) : ActivityTimeTrigger {
  override fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime {
    return Time.toLocalDateTime(timeMillis)
  }

  override fun toString(): String {
    return "FixedTimeInMillisActivityTrigger(timeMillis=$timeMillis)"
  }
}
