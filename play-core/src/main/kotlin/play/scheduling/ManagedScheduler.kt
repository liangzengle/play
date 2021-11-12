package play.scheduling

import com.github.benmanes.caffeine.cache.Caffeine
import mu.KLogging
import play.util.Cleaners
import play.util.unsafeCast
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

class ManagedScheduler(private val underlying: Scheduler) : Scheduler, AutoCloseable {
  private val schedules: MutableMap<Any, Canceller<*>> =
    Caffeine.newBuilder().weakKeys().build<Any, Canceller<*>>().asMap()

  init {
    Cleaners.register(this, Action(schedules))
  }

  override fun close() {
    Action.clean(schedules, "close")
  }

  private class Action(val schedules: Map<Any, Canceller<*>>) : Runnable {
    companion object : KLogging() {
      fun clean(schedules: Map<Any, Canceller<*>>, commander: String) {
        var n = 0
        for ((schedule, canceller) in schedules) {
          if (canceller.unsafeCast<Canceller<Any>>().cancel(schedule)) {
            n++
          }
        }
        logger.info { "$n schedules cancelled by $commander" }
      }
    }

    override fun run() {
      clean(schedules, "Cleaner")
    }
  }

  override fun schedule(delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(delay, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(delay, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAt(triggerTime: LocalDateTime, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTime, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAt(triggerTime: LocalDateTime, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTime, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAt(triggerTimeInMillis: Long, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTimeInMillis, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAt(triggerTimeInMillis: Long, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAt(triggerTimeInMillis, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleWithFixedDelay(delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(delay, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleWithFixedDelay(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(delay, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(initialDelay, delay, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    val cancellable = underlying.scheduleWithFixedDelay(initialDelay, delay, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAtFixedRate(interval: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(interval, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAtFixedRate(interval: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(interval, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAtFixedRate(initialDelay: Duration, interval: Duration, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(initialDelay, interval, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    val cancellable = underlying.scheduleAtFixedRate(initialDelay, interval, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleCron(cronExpr: String, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleCron(cronExpr: String, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun scheduleCron(
    cronExpr: String,
    task: Runnable,
    startTime: Optional<LocalDateTime>,
    endTime: Optional<LocalDateTime>
  ): Cancellable {
    val cancellable = underlying.scheduleCron(cronExpr, task, startTime, endTime)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
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
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun schedule(trigger: Trigger, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(trigger, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }

  override fun schedule(trigger: Trigger, taskExecutor: Executor, task: Runnable): Cancellable {
    val cancellable = underlying.schedule(trigger, taskExecutor, task)
    schedules[cancellable.taskHandle()] = cancellable.canceller()
    return cancellable
  }
}
