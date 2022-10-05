package play.example.game.app.module.activity.base.trigger

import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
class FixedTimeTrigger(private val time: LocalDateTime) : ActivityTimeTrigger {
  override fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime {
    return time
  }

  override fun toString(): String {
    return "FixedTimeTrigger(time=$time)"
  }
}
