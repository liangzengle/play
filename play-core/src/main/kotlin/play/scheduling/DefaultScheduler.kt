package play.scheduling

import play.scheduling.Cancellable.Companion.toCancellable
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 基于[ScheduledExecutorService]的定时器
 *
 * Created by LiangZengle on 2020/2/20.
 */
class DefaultScheduler(private val scheduleService: ScheduledExecutorService, executor: Executor, clock: Clock) :
  AbstractScheduler(executor, clock), ScheduledExecutorService by scheduleService {

  override fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduleService.schedule(
      { taskExecutor.execute(withErrorHandling(task, false)) },
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    ).toCancellable()
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    return scheduleService.scheduleWithFixedDelay(
      { taskExecutor.execute(withErrorHandling(task, true)) },
      initialDelay.toMillis(),
      delay.toMillis(),
      TimeUnit.MILLISECONDS
    ).toCancellable()
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    return scheduleService.scheduleAtFixedRate(
      { taskExecutor.execute(withErrorHandling(task, true)) },
      initialDelay.toMillis(),
      interval.toMillis(),
      TimeUnit.MILLISECONDS
    ).toCancellable()
  }
}
