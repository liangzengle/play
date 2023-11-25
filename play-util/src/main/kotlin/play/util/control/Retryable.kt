package play.util.control

import play.util.concurrent.PlayFuture
import play.util.logging.WithLogger
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
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
  private val task: () -> PlayFuture<Any?>
) {

  init {
    require(intervalMillis > 0) { "illegal intervalMillis: $intervalMillis" }
    require(attempts == -1L || attempts > 0) { "illegal attempts: $attempts" }
  }

  private val scheduler = ScheduledThreadPoolExecutor(1, Thread.ofVirtual().name(name).factory())

  @Volatile
  private var state = STATE_INIT

  private var attempted = -1L

  private fun run() {
    val future: PlayFuture<Any?> = try {
      task()
    } catch (e: Exception) {
      logger.info(e) { "[$this] attempt failed" }
      PlayFuture.failed(e)
    }
    future
      .onSuccess {
        logger.info { "[$this] succeeded: attempted $attempted" }
        scheduler.shutdown()
      }
      .onFailure { e ->
        if (attempts == -1L || attempts > attempted) {
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
    scheduler.schedule(this::run, delayMillis, TimeUnit.MILLISECONDS)
  }

  override fun toString(): String {
    return "Retry $name $attempts times every ${intervalMillis}ms"
  }

  companion object : WithLogger() {

    private const val STATE_INIT = 0
    private const val STATE_STARTED = 1

    @JvmStatic
    private val StateUpdater = AtomicIntegerFieldUpdater.newUpdater(Retryable::class.java, "state")

    /**
     * Retry until the future returned by [task] completes successfully
     */
    fun untilSuccess(
      name: String,
      intervalMillis: Long,
      task: () -> PlayFuture<Any?>
    ): Retryable = Retryable(name, -1L, intervalMillis) {
      PlayFuture.successful(task())
    }
  }
}
