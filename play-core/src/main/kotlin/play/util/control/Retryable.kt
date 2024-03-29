package play.util.control

import mu.KLogging
import play.scheduling.Scheduler
import play.util.concurrent.Future
import play.util.concurrent.PlayFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 * 失败重试
 *
 * @property name 任务名称
 * @property attempts 重试次数
 * @property intervalMillis 重试间隔(毫秒)
 * @property task 任务，根据future的状态判断是否成功
 */
@Suppress("unused")
class Retryable(
  val name: String,
  private val attempts: Long,
  private val intervalMillis: Long,
  private val scheduler: Scheduler,
  private val executor: Executor,
  private val task: () -> PlayFuture<Any?>
) {

  init {
    require(intervalMillis > 0) { "illegal intervalMillis: $intervalMillis" }
    require(attempts > 0) { "illegal attempts: $attempts" }
  }

  @Volatile
  private var state = STATE_INIT

  private var attempted = -1L

  private fun run() {
    val future: PlayFuture<Any?> = try {
      task()
    } catch (e: Exception) {
      logger.warn(e) { "[$this] attempt failed" }
      PlayFuture.failed(e)
    }
    future
      .onSuccess {
        logger.info { "[$this] succeeded: attempted $attempted" }
      }
      .onFailure { e ->
        if (attempts > attempted) {
          schedule(intervalMillis)
        } else {
          logger.warn(e) { "[$this] give up: attempts $attempts, attempted $attempted" }
        }
      }
  }

  /**
   * 启动任务
   * @param runImmediately 是否立即执行一次
   */
  fun start(runImmediately: Boolean = false) {
    if (!StateUpdater.compareAndSet(this, STATE_INIT, STATE_STARTED)) {
      throw IllegalStateException("[$this] started.")
    }
    val delay = if (runImmediately) 0 else intervalMillis
    schedule(delay)
  }

  private fun schedule(delayMillis: Long) {
    scheduler.schedule(java.time.Duration.ofMillis(delayMillis), executor, this::run)
  }

  override fun toString(): String {
    return "Retry $name $attempts times every ${intervalMillis}ms"
  }

  companion object : KLogging() {

    private const val STATE_INIT = 0
    private const val STATE_STARTED = 1

    @JvmStatic
    private val StateUpdater = AtomicIntegerFieldUpdater.newUpdater(Retryable::class.java, "state")

    fun forever(
      name: String,
      intervalMillis: Long,
      scheduler: Scheduler,
      executor: Executor,
      task: () -> Boolean
    ): Retryable = Retryable(name, Long.MAX_VALUE, intervalMillis, scheduler, executor) {
      Future.successful(task())
    }

    fun foreverAsync(
      name: String,
      intervalMillis: Long,
      scheduler: Scheduler,
      executor: Executor,
      task: () -> PlayFuture<Any?>
    ): Retryable = Retryable(name, Long.MAX_VALUE, intervalMillis, scheduler, executor, task)
  }
}
