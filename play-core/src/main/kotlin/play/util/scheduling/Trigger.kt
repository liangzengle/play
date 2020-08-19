package play.util.scheduling

import play.util.time.currentDateTime
import play.util.time.toDate
import play.util.time.toLocalDateTime
import java.time.LocalDateTime

/**
 * Created by LiangZengle on 2020/2/20.
 */
interface Trigger {
  fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime?
}

class PeriodCronTrigger(
  private val sequenceGenerator: CronSequenceGenerator,
  private val startTime: LocalDateTime?,
  private val stopTime: LocalDateTime?
) :
  Trigger {
  override fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime? {
    var date = triggerContext.lastCompletionTime()
    if (date != null) {
      val scheduled = triggerContext.lastScheduledExecutionTime()
      if (scheduled != null && date.isBefore(scheduled)) { // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled
      }
    } else {
      val now = currentDateTime()
      date = if (startTime?.isAfter(now) == true) startTime else now
    }
    val next = this.sequenceGenerator.next(date.toDate()).toLocalDateTime()
    return if (stopTime?.let { next.isAfter(it) } == true) null else next
  }
}
