package play.scheduling

import play.util.time.currentDateTime
import java.time.LocalDateTime

interface Trigger {
  fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime?
}

class CronTrigger(
  private val sequenceGenerator: CronExpression
) : Trigger {
  override fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime? {
    var date = triggerContext.lastCompletionTime()
    if (date != null) {
      val scheduled = triggerContext.lastScheduledExecutionTime()
      if (scheduled != null && date.isBefore(scheduled)) {
        // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled
      }
    } else {
      date = triggerContext.clock.currentDateTime()
    }
    return this.sequenceGenerator.nextFireTime(date)
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(${sequenceGenerator.getExpression()})"
  }
}

/**
 * 在一定时间区间内才有效的cron定时触发器
 *
 * @param sequenceGenerator CronExpression
 * @param startTime 起始时间（包含）
 * @param stopTime 结束时间（包含）
 */
class BoundedCronTrigger(
  private val sequenceGenerator: CronExpression,
  private val startTime: LocalDateTime?,
  private val stopTime: LocalDateTime?
) : Trigger {
  override fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime? {
    var date = triggerContext.lastCompletionTime()
    if (date != null) {
      val scheduled = triggerContext.lastScheduledExecutionTime()
      if (scheduled != null && date.isBefore(scheduled)) {
        // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled
      }
    } else {
      date = triggerContext.clock.currentDateTime()
    }
    var nextFireTime = this.sequenceGenerator.nextFireTime(date)
    if (startTime != null && nextFireTime.isBefore(startTime)) {
      nextFireTime = this.sequenceGenerator.nextFireTime(startTime.minusSeconds(1))
    }
    return if (stopTime != null && nextFireTime.isAfter(stopTime)) null else nextFireTime
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(cron=${sequenceGenerator.getExpression()}, startTime=${startTime?.toString() ?: "*"}, stopTime=${startTime?.toString() ?: "*"})"
  }
}
