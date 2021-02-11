package play.util.scheduling

import java.time.Duration
import java.util.concurrent.*
import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.Log
import play.getLogger
import play.util.concurrent.CommonPool
import play.util.concurrent.LoggingUncaughtExceptionHandler
import play.util.concurrent.NamedThreadFactory
import play.util.scheduling.Cancellable.Companion.toCancellable

/**
 * 基于[ScheduledExecutorService]的定时器
 *
 * Created by LiangZengle on 2020/2/20.
 */
class DefaultScheduler(private val scheduleService: ScheduledExecutorService) : Scheduler(CommonPool),
  ScheduledExecutorService by scheduleService {
  
  private val logger = getLogger()

  private fun decorate(task: () -> Unit, taskExecutor: Executor): Runnable = Runnable {
    taskExecutor.execute {
      try {
        task()
      } catch (e: Exception) {
        logger.error(e) { e.message }
      }
    }
  }

  override fun schedule(delay: Duration, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return scheduleService.schedule(decorate(task, taskExecutor), delay.toMillis(), TimeUnit.MILLISECONDS)
      .toCancellable()
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable {
    return scheduleService.scheduleWithFixedDelay(
      decorate(task, taskExecutor),
      initialDelay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    ).toCancellable()
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable {
    return scheduleService.scheduleAtFixedRate(
      decorate(task, taskExecutor),
      initialDelay.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    ).toCancellable()
  }
}

@Singleton
class DefaultSchedulerProvider @Inject constructor(@Nullable executorService: ScheduledExecutorService?) :
  Provider<Scheduler> {
  private val scheduler by lazy { DefaultScheduler(executorService ?: make()) }

  private fun make(): ScheduledExecutorService {
    val threadFactory = NamedThreadFactory.newBuilder("scheduled-executor")
      .daemon(true)
      .exceptionHandler(LoggingUncaughtExceptionHandler)
      .build()
    return object : ScheduledThreadPoolExecutor(1, threadFactory) {
      override fun afterExecute(r: Runnable?, t: Throwable?) {
        super.afterExecute(r, t)
        var ex = t
        if (ex == null && r is Future<*>) {
          try {
            if (r.isDone) r.get()
          } catch (e: CancellationException) {
            ex = e
          } catch (e: ExecutionException) {
            ex = e.cause
          } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
          }
        }
        if (ex != null) Log.error(ex) { "Exception occurred when running $r" }
      }
    }
  }

  override fun get(): Scheduler = scheduler
}
