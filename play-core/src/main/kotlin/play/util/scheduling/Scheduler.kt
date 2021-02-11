package play.util.scheduling

import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor
import play.util.getOrNull
import play.util.time.currentDateTime
import play.util.time.currentMillis

typealias PlayScheduler = Scheduler

/**
 * 定时器
 *
 * @author LiangZengle
 */
abstract class Scheduler(protected val workerPool: Executor) {

  fun schedule(delay: Duration, task: () -> Unit): Cancellable {
    return schedule(delay, workerPool, task)
  }

  abstract fun schedule(delay: Duration, taskExecutor: Executor, task: () -> Unit): Cancellable

  fun scheduleAt(triggerTime: LocalDateTime, task: () -> Unit): Cancellable {
    return scheduleAt(triggerTime, workerPool, task)
  }

  fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return schedule(Duration.between(currentDateTime(), triggerTime), taskExecutor, task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, task: () -> Unit): Cancellable {
    return scheduleAt(triggerTimeInMillis, workerPool, task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return schedule(Duration.ofMillis(currentMillis() - triggerTimeInMillis), taskExecutor, task)
  }

  fun scheduleWithFixedDelay(delay: Duration, task: () -> Unit): Cancellable {
    return scheduleWithFixedDelay(delay, workerPool, task)
  }

  fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return scheduleWithFixedDelay(delay, delay, taskExecutor, task)
  }

  fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: () -> Unit): Cancellable {
    return scheduleWithFixedDelay(initialDelay, delay, workerPool, task)
  }

  abstract fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable

  fun scheduleAtFixedRate(interval: Duration, task: () -> Unit): Cancellable {
    return scheduleAtFixedRate(interval, workerPool, task)
  }

  fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return scheduleAtFixedRate(interval, interval, taskExecutor, task)
  }

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    task: () -> Unit
  ): Cancellable {
    return scheduleAtFixedRate(initialDelay, interval, workerPool, task)
  }

  abstract fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable

  fun scheduleCron(cronExpr: String, task: () -> Unit): Cancellable {
    return scheduleCron(cronExpr, workerPool, task)
  }

  fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return scheduleCron(cronExpr, taskExecutor, task, Optional.empty(), Optional.empty())
  }

  fun scheduleCron(
    cronExpr: String,
    task: () -> Unit,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    return scheduleCron(cronExpr, workerPool, task, startTime, endTime)
  }

  fun scheduleCron(
    cronExpr: String,
    taskExecutor: Executor,
    task: () -> Unit,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, startTime.getOrNull(), endTime.getOrNull())
    return ReschedulingRunnable({ taskExecutor.execute(task) }, trigger, this, LoggingErrorHandler).schedule()
  }

  fun scheduleCron(
    trigger: Trigger,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable {
    return ReschedulingRunnable({ taskExecutor.execute(task) }, trigger, this, LoggingErrorHandler).schedule()
  }
}
