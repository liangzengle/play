package play.scheduling

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import play.time.Time.toLocalDateTime
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 *
 * @author LiangZengle
 */
class SpringTaskScheduler(private val scheduler: Scheduler) : TaskScheduler {

  override fun getClock(): Clock = scheduler.clock()

  override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*> {
    return scheduler.schedule(PlayTriggerAdapter(trigger), task).toScheduledFuture()
  }

  override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> {
    return scheduler.scheduleAt(startTime.toLocalDateTime(), task).toScheduledFuture()
  }

  override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration): ScheduledFuture<*> {
    val initialDelayMillis = startTime.toEpochMilli() - clock.millis()
    return scheduler.scheduleAtFixedRate(Duration.ofMillis(initialDelayMillis), period, task)
      .toScheduledFuture()
  }

  override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> {
    return scheduler.scheduleAtFixedRate(Duration.ZERO, period, task).toScheduledFuture()
  }

  override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration): ScheduledFuture<*> {
    val initialDelayMillis = startTime.toEpochMilli() - clock.millis()
    return scheduler.scheduleWithFixedDelay(Duration.ofMillis(initialDelayMillis), delay, task)
      .toScheduledFuture()
  }

  override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> {
    return scheduler.scheduleWithFixedDelay(Duration.ZERO, delay, task).toScheduledFuture()
  }

  private class PlayTriggerAdapter(val spring: Trigger) : play.scheduling.Trigger {
    override fun nextExecutionTime(triggerContext: TriggerContext): Instant? {
      return spring.nextExecution(SpringTriggerContextAdapter(triggerContext))
    }
  }

  private class SpringTriggerContextAdapter(
    val play: TriggerContext
  ) : org.springframework.scheduling.TriggerContext {

    override fun lastScheduledExecution(): Instant? {
      return play.lastActualExecution()
    }

    override fun lastActualExecution(): Instant? {
      return play.lastActualExecution()
    }

    override fun lastCompletion(): Instant? {
      return play.lastCompletion()
    }
  }
}
