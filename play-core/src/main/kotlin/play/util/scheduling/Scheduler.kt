package play.util.scheduling

import play.getLogger
import play.util.concurrent.CommonPool
import play.util.getOrNull
import play.util.scheduling.executor.ScheduledExecutor
import play.util.time.currentDateTime
import play.util.time.currentMillis
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by LiangZengle on 2020/2/20.
 */
@Singleton
class Scheduler {
  private val logger = getLogger()

  private val executor = ScheduledExecutor.get()

  private val workerPool = CommonPool

  private fun decorate(task: () -> Unit, ec: Executor = workerPool): Runnable = Runnable {
    ec.execute {
      try {
        task()
      } catch (e: Exception) {
        logger.error(e) { e.message }
      }
    }
  }

  fun schedule(delay: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.schedule(decorate(task), delay.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun schedule(delay: Duration, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return executor.schedule(decorate(task, ec), delay.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun scheduleAt(triggerTime: LocalDateTime, task: () -> Unit): ScheduledFuture<*> {
    return schedule(Duration.between(currentDateTime(), triggerTime), task)
  }

  fun scheduleAt(triggerTime: LocalDateTime, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return schedule(Duration.between(currentDateTime(), triggerTime), ec, task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, task: () -> Unit): ScheduledFuture<*> {
    return schedule(Duration.ofMillis(currentMillis() - triggerTimeInMillis), task)
  }

  fun scheduleAt(triggerTimeInMillis: Long, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return schedule(Duration.ofMillis(currentMillis() - triggerTimeInMillis), ec, task)
  }

  fun scheduleWithFixedDelay(delay: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.scheduleWithFixedDelay(
      decorate(task),
      delay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleWithFixedDelay(delay: Duration, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return executor.scheduleWithFixedDelay(
      decorate(task, ec),
      delay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleWithFixedDelay(initialDelay: Duration, delay: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.scheduleWithFixedDelay(
      decorate(task),
      initialDelay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    ec: Executor,
    task: () -> Unit
  ): ScheduledFuture<*> {
    return executor.scheduleWithFixedDelay(
      decorate(task, ec),
      initialDelay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleAtFixedRate(
    interval: Duration,
    task: () -> Unit
  ): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task),
      interval.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleAtFixedRate(
    interval: Duration,
    ec: Executor,
    task: () -> Unit
  ): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task, ec),
      interval.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    task: () -> Unit
  ): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task),
      initialDelay.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    ec: Executor,
    task: () -> Unit
  ): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task, ec),
      initialDelay.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleCron(cronExpr: String, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, null, null)
    return ReschedulingRunnable(decorate(task, ec), trigger, executor, LoggingErrorHandler).schedule()!!
  }

  fun scheduleCron(
    cronExpr: String,
    ec: Executor,
    task: () -> Unit,
    startTime: Optional<LocalDateTime> = Optional.empty(),
    stopTime: Optional<LocalDateTime> = Optional.empty()
  ): ScheduledFuture<*>? {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, startTime.getOrNull(), stopTime.getOrNull())
    return ReschedulingRunnable(decorate(task, ec), trigger, executor, LoggingErrorHandler).schedule()
  }

  fun scheduleCron(
    cronExpr: String,
    task: () -> Unit,
    startTime: Optional<LocalDateTime> = Optional.empty(),
    stopTime: Optional<LocalDateTime> = Optional.empty()
  ): ScheduledFuture<*>? {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, startTime.getOrNull(), stopTime.getOrNull())
    return ReschedulingRunnable(decorate(task), trigger, executor, LoggingErrorHandler).schedule()
  }

  fun scheduleCron(cronExpr: String, task: () -> Unit): ScheduledFuture<*> {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, null, null)
    return ReschedulingRunnable(decorate(task), trigger, executor, LoggingErrorHandler).schedule()!!
  }
}

@Singleton
class SchedulerProvider : Provider<Scheduler> {
  private val scheduler by lazy { Scheduler() }
  override fun get(): Scheduler = scheduler
}
