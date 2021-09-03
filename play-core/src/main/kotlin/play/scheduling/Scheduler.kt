package play.scheduling

import play.util.getOrNull
import play.util.time.Time.currentDateTime
import play.util.time.Time.currentMillis
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

typealias PlayScheduler = Scheduler

/**
 * 定时器
 *
 * @author LiangZengle
 */
abstract class Scheduler(private val workerPool: Executor, val clock: Clock) {

  fun withErrorHandling(task: Runnable, isRepeated: Boolean): Runnable =
    DelegatingErrorHandlingRunnable(task, TaskUtils.getDefaultErrorHandler(isRepeated))

  fun schedule(delay: Duration, task: Runnable): Cancellable {
    return schedule(delay, workerPool, task)
  }

  abstract fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleAt(triggerTime: LocalDateTime, task: Runnable): Cancellable {
    return scheduleAt(triggerTime, workerPool, task)
  }

  fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: Runnable): Cancellable {
    return schedule(Duration.between(currentDateTime(), triggerTime), taskExecutor, task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, task: Runnable): Cancellable {
    return scheduleAt(triggerTimeInMillis, workerPool, task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: Runnable): Cancellable {
    return schedule(Duration.ofMillis(currentMillis() - triggerTimeInMillis), taskExecutor, task)
  }

  fun scheduleWithFixedDelay(delay: Duration, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(delay, workerPool, task)
  }

  fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(delay, delay, taskExecutor, task)
  }

  fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Cancellable {
    return scheduleWithFixedDelay(initialDelay, delay, workerPool, task)
  }

  abstract fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable

  fun scheduleAtFixedRate(interval: Duration, task: Runnable): Cancellable {
    return scheduleAtFixedRate(interval, workerPool, task)
  }

  fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleAtFixedRate(interval, interval, taskExecutor, task)
  }

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    task: Runnable
  ): Cancellable {
    return scheduleAtFixedRate(initialDelay, interval, workerPool, task)
  }

  abstract fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable

  fun scheduleCron(cronExpr: String, task: Runnable): Cancellable {
    return scheduleCron(cronExpr, workerPool, task)
  }

  fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleCron(cronExpr, taskExecutor, task, Optional.empty(), Optional.empty())
  }

  fun scheduleCron(
    cronExpr: String,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    return scheduleCron(cronExpr, workerPool, task, startTime, endTime)
  }

  fun scheduleCron(
    cronExpr: String,
    taskExecutor: Executor,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val sequenceGenerator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(sequenceGenerator, startTime.getOrNull(), endTime.getOrNull())
    return schedule(trigger, taskExecutor, task)
  }

  fun schedule(
    trigger: Trigger,
    task: Runnable
  ): Cancellable {
    return schedule(trigger, workerPool, task)
  }

  fun schedule(
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
