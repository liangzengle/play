package play.akka.scheduling

import akka.actor.typed.Scheduler
import play.scheduling.AbstractScheduler
import play.scheduling.Cancellable
import scala.concurrent.ExecutionContextExecutor
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executor

/**
 *
 * @author LiangZengle
 */
class AkkaScheduler(
  private val scheduler: Scheduler,
  private val ec: ExecutionContextExecutor,
  clock: Clock
) : AbstractScheduler(ec, clock) {

  private fun akka.actor.Cancellable.toPlay(): Cancellable = object : Cancellable {
    override fun cancel(): Boolean = this@toPlay.cancel()

    override fun isCancelled(): Boolean = this@toPlay.isCancelled

    override fun canceller(): AkkaCancellableCanceller = AkkaCancellableCanceller

    override fun taskHandle(): akka.actor.Cancellable = this@toPlay
  }

  override fun schedule(delay: Duration, taskExecutor: Executor, task: Runnable): Cancellable {
    return scheduler.scheduleOnce(delay, withErrorHandling(task, false), ec).toPlay()
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    return scheduler.scheduleWithFixedDelay(initialDelay, delay, withErrorHandling(task, true), ec).toPlay()
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: Runnable
  ): Cancellable {
    return scheduler.scheduleAtFixedRate(initialDelay, interval, withErrorHandling(task, true), ec).toPlay()
  }
}
