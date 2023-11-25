package play.scheduling

import play.util.time.Time.toInstant
import play.util.time.instantNotNull
import java.time.Instant
import java.time.LocalDateTime

interface Trigger {
  fun nextExecutionTime(triggerContext: TriggerContext): Instant?
}

class CronTrigger(
  private val sequenceGenerator: CronExpression
) : Trigger {
  override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
    var date = triggerContext.lastCompletion()
    if (date != null) {
      val scheduled = triggerContext.lastScheduledExecution()
      if (scheduled != null && date.isBefore(scheduled)) {
        // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled
      }
    } else {
      date = triggerContext.clock.instantNotNull()
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
  override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
    var date = triggerContext.lastCompletion()
    if (date != null) {
      val scheduled = triggerContext.lastScheduledExecution()
      if (scheduled != null && date.isBefore(scheduled)) {
        // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled
      }
    } else {
      date = triggerContext.clock.instantNotNull()
    }
    var nextFireTime = this.sequenceGenerator.nextFireTime(date)
    val startInstant = startTime?.run { toInstant() }
    if (startInstant != null && nextFireTime.isBefore(startInstant)) {
      nextFireTime = this.sequenceGenerator.nextFireTime(startInstant.minusSeconds(1))
    }
    val stopInstant = stopTime?.run { toInstant() }
    return if (stopInstant != null && nextFireTime.isAfter(stopInstant)) null else nextFireTime
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(cron=${sequenceGenerator.getExpression()}, startTime=${startTime?.toString() ?: "*"}, stopTime=${startTime?.toString() ?: "*"})"
  }
}
