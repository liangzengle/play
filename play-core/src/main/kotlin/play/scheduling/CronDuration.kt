package play.scheduling

import play.time.Time
import play.time.Time.toInstant
import java.time.Duration
import java.time.LocalDateTime

class CronDuration(startCron: String, val duration: Duration) {
  val startCronExpr = CronExpression.parse(startCron)

  fun isInDuration(time: LocalDateTime): Boolean {
    val timeInstant = time.toInstant()
    val startTime = startCronExpr.prevFireTime(timeInstant) ?: return false
    val endTime = startTime.plus(duration)
    return startTime <= timeInstant && endTime > timeInstant
  }

  fun isInDurationNow(): Boolean {
    return isInDuration(Time.currentDateTime())
  }

  override fun toString(): String {
    return "CronDuration(startCronExpr=$startCronExpr, duration=$duration)"
  }
}
