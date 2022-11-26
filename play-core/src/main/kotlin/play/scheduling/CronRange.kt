package play.scheduling

import play.util.time.Time
import play.util.time.Time.toInstant
import java.time.Duration
import java.time.LocalDateTime

class CronRange(startCron: String, endCron: String) {
  val startCronExpr = CronExpression.parse(startCron)
  val endCronExpr = CronExpression.parse(endCron)
  val duration: Duration

  init {
    val startTime = startCronExpr.nextFireTime(Time.instant())
    val endTime = endCronExpr.nextFireTime(startTime)
    duration = Duration.between(startTime, endTime)
  }

  fun isInRangeNow(): Boolean {
    return isInRange(Time.currentDateTime())
  }

  fun isInRange(time: LocalDateTime): Boolean {
    val timeInstant = time.toInstant()
    val endTime = endCronExpr.nextFireTime(timeInstant)
    val startTime = startCronExpr.prevFireTime(endTime)
    return (startTime != null && startTime <= timeInstant) && endTime > timeInstant
  }

  override fun toString(): String {
    return "CronRange(startCron=${startCronExpr.getExpression()}, endCron=${endCronExpr.getExpression()}, duration=$duration)"
  }
}
