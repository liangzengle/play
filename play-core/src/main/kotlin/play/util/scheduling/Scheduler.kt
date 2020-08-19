package play.util.scheduling

import io.vavr.control.Option
import play.getLogger
import play.util.concurrent.threadFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by LiangZengle on 2020/2/20.
 */
@Singleton
class Scheduler @Inject constructor(val executor: ScheduledExecutorService) {
  companion object {
    @JvmStatic
    private val logger = getLogger()
  }

  private val workerPool = ThreadPoolExecutor(
    1,
    Int.MAX_VALUE,
    60L,
    TimeUnit.SECONDS,
    SynchronousQueue<Runnable>(),
    threadFactory("schedule-worker", true),
    ThreadPoolExecutor.CallerRunsPolicy()
  )

  private fun decorate(task: () -> Unit, ec: Executor = workerPool): Runnable = Runnable {
    ec.execute {
      try {
        task()
      } catch (e: Exception) {
        logger.error(e) { e.message }
      }
    }
  }

  fun scheduleOnce(delay: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.schedule(decorate(task), delay.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun scheduleRepeatedly(interval: Duration, task: () -> Unit): ScheduledFuture<*> {
    return scheduleRepeatedly(interval, interval, task)
  }

  fun scheduleRepeatedly(initialDelay: Duration, interval: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task),
      initialDelay.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    )
  }

  fun scheduleAtFixedRate(initialDelay: Duration, interval: Duration, task: () -> Unit): ScheduledFuture<*> {
    return executor.scheduleAtFixedRate(
      decorate(task),
      initialDelay.toMillis(),
      interval.toMillis(),
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

  fun scheduleCron(cronExpr: String, task: () -> Unit): ScheduledFuture<*> {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, null, null)
    return ReschedulingRunnable(decorate(task), trigger, executor, LoggingErrorHandler).schedule()!!
  }

  fun scheduleCron(
    cronExpr: String,
    task: () -> Unit,
    startTime: Option<LocalDateTime> = Option.none(),
    stopTime: Option<LocalDateTime> = Option.none()
  ): ScheduledFuture<*>? {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, startTime.orNull, stopTime.orNull)
    return ReschedulingRunnable(decorate(task), trigger, executor, LoggingErrorHandler).schedule()
  }


  fun scheduleOnce(delay: Duration, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return executor.schedule(decorate(task, ec), delay.toMillis(), TimeUnit.MILLISECONDS)
  }

  fun scheduleRepeatedly(interval: Duration, ec: Executor, task: () -> Unit): ScheduledFuture<*> {
    return scheduleRepeatedly(interval, interval, ec, task)
  }

  fun scheduleRepeatedly(
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

  fun scheduleCron(cronExpr: String, ec: ExecutorService, task: () -> Unit): ScheduledFuture<*> {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, null, null)
    return ReschedulingRunnable(decorate(task, ec), trigger, executor, LoggingErrorHandler).schedule()!!
  }

  fun scheduleCron(
    cronExpr: String,
    ec: Executor,
    task: () -> Unit,
    startTime: Option<LocalDateTime> = Option.none(),
    stopTime: Option<LocalDateTime> = Option.none()
  ): ScheduledFuture<*>? {
    val generator = CronSequenceGenerator(cronExpr)
    val trigger = PeriodCronTrigger(generator, startTime.orNull, stopTime.orNull)
    return ReschedulingRunnable(decorate(task, ec), trigger, executor, LoggingErrorHandler).schedule()
  }
}
