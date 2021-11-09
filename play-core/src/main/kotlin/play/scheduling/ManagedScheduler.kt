package play.scheduling

import mu.KLogging
import play.util.Cleaners
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

class ManagedScheduler(private val underlying: Scheduler) : Scheduler, AutoCloseable {
  private val schedules = Collections.synchronizedMap(WeakHashMap<Any, Cancellable>())

  private val cleanable = Cleaners.register(this, Action(schedules))

  override fun close() {
    cleanable.clean()
  }

  private class Action(val schedules: Map<Any, Cancellable>) : Runnable {
    companion object : KLogging()

    override fun run() {
      var n = 0
      for (cancellable in schedules.values) {
        if (!cancellable.isCancelled()) {
          cancellable.cancel()
          n++
        }
      }
      logger.info { "$n schedules cancelled by Cleaner" }
    }
  }

  override fun schedule(delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(delay, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(delay, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAt(triggerTime: LocalDateTime, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTime, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTime, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAt(triggerTimeInMillis: Long, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTimeInMillis, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTimeInMillis, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleWithFixedDelay(delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(delay, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(delay, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(initialDelay, delay, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(initialDelay, delay, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAtFixedRate(interval: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(interval, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(interval, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAtFixedRate(initialDelay: Duration, interval: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(initialDelay, interval, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(initialDelay, interval, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleCron(cronExpr: String, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleCron(
    cronExpr: String,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, task, startTime, endTime)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun scheduleCron(
    cronExpr: String,
    taskExecutor: Executor,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, taskExecutor, task, startTime, endTime)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun schedule(trigger: Trigger, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(trigger, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }

  override fun schedule(trigger: Trigger, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(trigger, taskExecutor, task)
    schedules[cancellable.unwrap()] = cancellable
    return cancellable
  }
}
