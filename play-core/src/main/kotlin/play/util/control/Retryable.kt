package play.util.control

import play.getLogger
import play.util.concurrent.CommonPool
import play.util.scheduling.executor.ScheduledExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import kotlin.time.Duration

/**
 * 失败重试
 *
 * @property name 任务名称
 * @property attempts 重试次数, -1表示无限重试
 * @property intervalMillis 重试间隔(毫秒)
 * @property task 任务，返回true：成功，不再重试 false：失败，继续重试
 */
class Retryable(
  val name: String,
  private val attempts: Int,
  private val intervalMillis: Long,
  private val task: () -> Boolean
) : Runnable {

  constructor(name: String, attempts: Int, interval: Duration, task: () -> Boolean) : this(
    name,
    attempts,
    interval.toLongMilliseconds(),
    task
  )


  init {
    require(intervalMillis > 0) { "`attemptInterval`" }
    require(attempts >= -1) { "`attempts`" }
  }

  @Volatile
  private var attempted = -1

  override fun run() {
    val attempted = this.attempted
    if (attempted == -1) {
      throw IllegalStateException("$this not started.")
    }
    var succeed = false
    try {
      succeed = task()
    } catch (e: Exception) {
      logger.debug(e) { "[$this] attempt failed" }
    } finally {
      val nowAttempted = attempted + 1
      if (!AttemptedUpdater.compareAndSet(this, attempted, nowAttempted)) {
        throw ConcurrentModificationException("should not happen")
      }
      if (succeed) {
        logger.info { "[$this] succeeded: attempted $nowAttempted" }
      } else {
        if (attempts == -1 || attempts > nowAttempted) {
          schedule(intervalMillis)
        } else {
          logger.info { "[$this] give up: attempts $attempts, attempted $nowAttempted" }
        }
      }
    }
  }

  fun start(immediately: Boolean) {
    if (attempts == 0) {
      return
    }
    if (!AttemptedUpdater.compareAndSet(this, -1, 0)) {
      throw IllegalStateException("[$this] started.")
    }
    val delay = if (immediately) 0 else intervalMillis
    schedule(delay)
  }

  private fun schedule(delayMillis: Long) {
    ScheduledExecutor.get().schedule({ CommonPool.submit(this) }, delayMillis, TimeUnit.MILLISECONDS)
  }

  override fun toString(): String {
    return "Retry $name $attempts times every ${intervalMillis}ms"
  }

  companion object {
    private val logger = getLogger()

    @JvmStatic
    private val AttemptedUpdater = AtomicIntegerFieldUpdater.newUpdater(Retryable::class.java, "attempted")

    fun forever(
      name: String,
      intervalMillis: Long,
      task: () -> Boolean
    ): Retryable = Retryable(name, -1, intervalMillis, task)
  }
}
