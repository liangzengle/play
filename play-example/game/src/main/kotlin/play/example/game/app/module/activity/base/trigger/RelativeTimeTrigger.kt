package play.example.game.app.module.activity.base.trigger

import java.time.Duration
import java.time.LocalDateTime

/**
 *
 * @author LiangZengle
 */
class RelativeTimeTrigger(private val base: ActivityBaseTimeKey, private val offset: Duration) : ActivityTimeTrigger {
  override fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime? {
    return ctx.getBaseTime(base)?.plus(offset)
  }

  override fun toString(): String {
    return "RelativeTimeTrigger(base=$base, offset=$offset)"
  }
}
