package play.example.common.akka.scheduling

import akka.actor.typed.Scheduler
import java.time.Duration
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import play.util.scheduling.Cancellable
import play.util.scheduling.PlayScheduler
import scala.concurrent.ExecutionContextExecutor

/**
 *
 * @author LiangZengle
 */
@Singleton
class AkkaScheduler @Inject constructor(
  private val scheduler: Scheduler,
  private val ec: ExecutionContextExecutor
) : PlayScheduler(ec) {

  @Suppress("NOTHING_TO_INLINE")
  private inline fun akka.actor.Cancellable.toPlay(): Cancellable = object : Cancellable {
    override fun cancel(): Boolean = this@toPlay.cancel()

    override fun isCancelled(): Boolean = this@toPlay.isCancelled
  }

  override fun schedule(delay: Duration, taskExecutor: Executor, task: () -> Unit): Cancellable {
    return scheduler.scheduleOnce(delay, task, ec).toPlay()
  }

  override fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable {
    return scheduler.scheduleWithFixedDelay(initialDelay, delay, task, ec).toPlay()
  }

  override fun scheduleAtFixedRate(
    initialDelay: Duration,
    interval: Duration,
    taskExecutor: Executor,
    task: () -> Unit
  ): Cancellable {
    return scheduler.scheduleAtFixedRate(initialDelay, interval, task, ec).toPlay()
  }
}
