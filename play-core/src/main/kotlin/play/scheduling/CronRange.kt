package play.scheduling

import play.util.time.Time
import java.time.Duration
import java.time.LocalDateTime

class CronRange(startCron: String, endCron: String) {
  private val startCronExpr = CronSequenceGenerator(startCron)
  private val endCronExpr = CronSequenceGenerator(endCron)
  private val duration: Duration

  init {
    val startTime = startCronExpr.nextFireTime(Time.currentDateTime())
    val endTime = endCronExpr.nextFireTime(startTime)
    duration = Duration.between(startTime, endTime)
  }

  fun isNowInRange(): Boolean {
    return isInRange(Time.currentDateTime())
  }

  fun isInRange(time: LocalDateTime): Boolean {
    val endTime = endCronExpr.nextFireTime(time)
    val startTime = startCronExpr.prevFireTime(endTime).orElseThrow()
    return startTime <= time && endTime > time
  }

  override fun toString(): String {
    return "CronRange(startCron=${startCronExpr.expression}, endCron=${endCronExpr.expression}, duration=$duration)"
  }
}
