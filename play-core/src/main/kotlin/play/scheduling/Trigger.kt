package play.scheduling

import play.util.time.Time.currentDateTime
import java.time.LocalDateTime

interface Trigger {
  fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime?
}

open class CronTrigger(
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
      date = currentDateTime()
    }
    return this.sequenceGenerator.nextFireTime(date)
  }
}

class BoundedCronTrigger(
  sequenceGenerator: CronExpression,
  private val startTime: LocalDateTime?,
  private val stopTime: LocalDateTime?
) : CronTrigger(sequenceGenerator) {
  override fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime? {
    val time = super.nextExecutionTime(triggerContext) ?: return null
    if (startTime != null && time.isBefore(startTime)) {
      return null
    }
    if (stopTime != null && !time.isBefore(stopTime)) {
      return null
    }
    return time
  }
}
