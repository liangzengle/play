package play.example.game.app.module.activity.base.trigger

import play.scheduling.CronExpression
import java.time.LocalDateTime

/**
 *
 *
 * @author LiangZengle
 */
class CronTimeTrigger(private val cron: String) : ActivityTimeTrigger {
  private val expr = CronExpression.parse(cron)

  override fun nextTriggerTime(baseTime: LocalDateTime, ctx: ActivityTimeTriggerContext): LocalDateTime {
    return expr.nextFireTime(baseTime)
  }

  override fun toString(): String {
    return "CronActivityTrigger($cron)"
  }
}
