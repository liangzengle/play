package play.scheduling

import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

typealias PlayScheduler = Scheduler

interface Scheduler {

  fun clock(): Clock

  fun schedule(delay: Duration, task: Runnable): Cancellable

  fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleAt(triggerTime: LocalDateTime, task: Runnable): Cancellable

  fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleAt(triggerTimeInMillis: Long, task: Runnable): Cancellable

  fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleWithFixedDelay(delay: Duration, task: Runnable): Cancellable

  fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Cancellable

  fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable

  fun scheduleAtFixedRate(interval: Duration, task: Runnable): Cancellable

  fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    task: Runnable
  ): Cancellable

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable

  fun scheduleCron(cronExpr: String, task: Runnable): Cancellable

  fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: Runnable): Cancellable

  fun scheduleCron(
    cronExpr: String,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable

  fun scheduleCron(
    cronExpr: String,
    taskExecutor: Executor,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable

  fun schedule(
    trigger: Trigger,
    task: Runnable
  ): Cancellable

  fun schedule(
    trigger: Trigger,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable
}
