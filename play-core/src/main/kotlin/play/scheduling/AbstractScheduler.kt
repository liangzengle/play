package play.scheduling

import play.util.getOrNull
import play.util.time.Time.currentDateTime
import play.util.time.Time.currentMillis
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

/**
 * 定时器
 *
 * @author LiangZengle
 */
abstract class AbstractScheduler(private val workerPool: Executor, val clock: Clock) : Scheduler {

  fun withErrorHandling(task: Runnable, isRepeated: Boolean): Runnable =
    DelegatingErrorHandlingRunnable(task, TaskUtils.getDefaultErrorHandler(isRepeated))

  override fun clock(): Clock = clock

  override fun schedule(delay: Duration, task: Runnable): Cancellable {
    return schedule(delay, workerPool, task)
  }

  override fun scheduleAt(triggerTime: LocalDateTime, task: Runnable): Cancellable {
    return scheduleAt(triggerTime, workerPool, task)
  }

  override fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: Runnable): Cancellable {
    return schedule(Duration.between(currentDateTime(), triggerTime), taskExecutor, task)
  }

  override fun scheduleAt(triggerTimeInMillis: Long, task: Runnable): Cancellable {
    return scheduleAt(triggerTimeInMillis, workerPool, task)
  }

  override fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: Runnable): Cancellable {
    return schedule(Duration.ofMillis(currentMillis() - triggerTimeInMillis), taskExecutor, task)
  }

  override fun scheduleWithFixedDelay(delay: Duration, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(delay, workerPool, task)
  }

  override fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(delay, delay, taskExecutor, task)
  }

  override fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(initialDelay, delay, workerPool, task)
  }

  override fun scheduleAtFixedRate(interval: Duration, task: Runnable): Cancellable {
    return scheduleAtFixedRate(interval, workerPool, task)
  }

  override fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleAtFixedRate(interval, interval, taskExecutor, task)
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    task: Runnable
  ): Cancellable {
    return scheduleAtFixedRate(initialDelay, interval, workerPool, task)
  }

  override fun scheduleCron(cronExpr: String, task: Runnable): Cancellable {
    return scheduleCron(cronExpr, workerPool, task)
  }

  override fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleCron(cronExpr, taskExecutor, task, Optional.empty(), Optional.empty())
  }

  override fun scheduleCron(
    cronExpr: String,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    return scheduleCron(cronExpr, workerPool, task, startTime, endTime)
  }

  override fun scheduleCron(
    cronExpr: String,
    taskExecutor: Executor,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val sequenceGenerator = CronExpression.parse(cronExpr)
    val trigger = BoundedCronTrigger(sequenceGenerator, startTime.getOrNull(), endTime.getOrNull())
    return schedule(trigger, taskExecutor, task)
  }

  override fun schedule(
    trigger: Trigger,
    task: Runnable
  ): Cancellable {
    return schedule(trigger, workerPool, task)
  }

  override fun schedule(
    trigger: Trigger,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    return ReschedulingRunnable(
      { taskExecutor.execute(task) },
      trigger,
      clock,
      this,
      TaskUtils.getDefaultErrorHandler(true)
    ).schedule()
  }
}
