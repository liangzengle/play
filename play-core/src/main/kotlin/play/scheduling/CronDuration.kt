package play.scheduling

import play.util.time.Time
import java.time.Duration
import java.time.LocalDateTime

class CronDuration(startCron: String, val duration: Duration) {
  val startCronExpr = CronExpression.parse(startCron)

  fun isInDuration(time: LocalDateTime): Boolean {
    val startTime = startCronExpr.prevFireTime(time) ?: return false
    val endTime = startTime.plus(duration)
    return startTime <= time && endTime > time
  }

  fun isInDurationNow(): Boolean {
    return isInDuration(Time.currentDateTime())
  }

  override fun toString(): String {
    return "CronDuration(startCronExpr=$startCronExpr, duration=$duration)"
  }
}
