package play.scheduling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import play.time.Time
import play.time.Time.toLocalDateTime
import play.time.currentDateTime

internal class BoundedCronTriggerTest {

  @Test
  fun startTime() {
    val context = SimpleTriggerContext(Time.clock())
    val startTime = context.clock.currentDateTime().plusDays(1).withNano(0)
    val trigger = BoundedCronTrigger(CronExpression.parse("* * * * * ?"), startTime, null)
    val nextExecutionTime = trigger.nextExecutionTime(context)

    assertEquals(nextExecutionTime?.toLocalDateTime(), startTime)
  }

  @Test
  fun stopTime() {
    val context = SimpleTriggerContext(Time.clock())
    val stopTime = context.clock.currentDateTime().withNano(0)
    val trigger = BoundedCronTrigger(CronExpression.parse("* * * * * ?"), null, stopTime)
    val nextExecutionTime = trigger.nextExecutionTime(context)

    assertNull(nextExecutionTime)
  }
}
