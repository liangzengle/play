package play.scheduling

import play.util.time.Time
import java.time.Duration
import java.time.LocalDateTime

class CronRange(startCron: String, endCron: String) {
  val startCronExpr = CronExpression.parse(startCron)
  val endCronExpr = CronExpression.parse(endCron)
  val duration: Duration

  init {
    val startTime = startCronExpr.nextFireTime(Time.currentDateTime())
    val endTime = endCronExpr.nextFireTime(startTime)
    duration = Duration.between(startTime, endTime)
  }

  fun isInRangeNow(): Boolean {
    return isInRange(Time.currentDateTime())
  }

  fun isInRange(time: LocalDateTime): Boolean {
    val endTime = endCronExpr.nextFireTime(time)
    val startTime = startCronExpr.prevFireTime(endTime)
    return (startTime != null && startTime <= time) && endTime > time
  }

  override fun toString(): String {
    return "CronRange(startCron=${startCronExpr.getExpression()}, endCron=${endCronExpr.getExpression()}, duration=$duration)"
  }
}
