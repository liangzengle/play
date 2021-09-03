package play.scheduling

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import play.util.time.Time.toDate
import play.util.time.Time.toLocalDateTime
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ScheduledFuture

/**
 *
 * @author LiangZengle
 */
internal class SpringTaskScheduler(private val scheduler: Scheduler, private val _clock: Clock) : TaskScheduler {

  override fun getClock(): Clock {
    return _clock
  }

  override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*>? {
    return scheduler.schedule(PlayTriggerAdapter(trigger), task).toScheduledFuture()
  }

  override fun schedule(task: Runnable, startTime: Date): ScheduledFuture<*> {
    return scheduler.scheduleAt(startTime.time, task).toScheduledFuture()
  }

  override fun scheduleAtFixedRate(task: Runnable, startTime: Date, period: Long): ScheduledFuture<*> {
    val initialDelayMillis = startTime.time - clock.millis()
    return scheduler.scheduleAtFixedRate(Duration.ofMillis(initialDelayMillis), Duration.ofMillis(period), task)
      .toScheduledFuture()
  }

  override fun scheduleAtFixedRate(task: Runnable, period: Long): ScheduledFuture<*> {
    return scheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofMillis(period), task).toScheduledFuture()
  }

  override fun scheduleWithFixedDelay(task: Runnable, startTime: Date, delay: Long): ScheduledFuture<*> {
    val initialDelayMillis = startTime.time - clock.millis()
    return scheduler.scheduleWithFixedDelay(Duration.ofMillis(initialDelayMillis), Duration.ofMillis(delay), task)
      .toScheduledFuture()
  }

  override fun scheduleWithFixedDelay(task: Runnable, delay: Long): ScheduledFuture<*> {
    return scheduler.scheduleWithFixedDelay(Duration.ZERO, Duration.ofMillis(delay), task).toScheduledFuture()
  }

  private class PlayTriggerAdapter(val spring: Trigger) : play.scheduling.Trigger {
    override fun nextExecutionTime(triggerContext: TriggerContext): LocalDateTime? {
      return spring.nextExecutionTime(SpringTriggerContextAdapter(triggerContext))?.toLocalDateTime()
    }
  }

  private class SpringTriggerContextAdapter(
    val play: TriggerContext
  ) : org.springframework.scheduling.TriggerContext {
    override fun lastScheduledExecutionTime(): Date? {
      return play.lastActualExecutionTime()?.toDate()
    }

    override fun lastActualExecutionTime(): Date? {
      return play.lastActualExecutionTime()?.toDate()
    }

    override fun lastCompletionTime(): Date? {
      return play.lastCompletionTime()?.toDate()
    }

  }
}
