package play.util.control

import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import kotlin.time.Duration
import play.getLogger
import play.util.concurrent.CommonPool
import play.util.scheduling.executor.ScheduledExecutor

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
  private val intervalMillis: Int,
  private val executor: Executor,
  private val task: () -> Boolean
) : Runnable {

  constructor(
    name: String,
    attempts: Int,
    interval: Duration,
    executor: Executor = CommonPool,
    task: () -> Boolean
  ) : this(
    name,
    attempts,
    interval.toLongMilliseconds().toInt(),
    executor,
    task
  )

  constructor(
    name: String,
    attempts: Int,
    interval: java.time.Duration,
    executor: Executor = CommonPool,
    task: () -> Boolean
  ) : this(
    name,
    attempts,
    interval.toMillis().toInt(),
    executor,
    task
  )

  init {
    require(intervalMillis > 0) { "`attemptInterval`" }
    require(attempts >= -1) { "`attempts`" }
  }

  @Volatile
  private var attempted = -1L

  override fun run() {
    val attempted = this.attempted
    if (attempted == -1L) {
      throw IllegalStateException("$this not started, call `start` instead.")
    }
    var succeed = false
    try {
      succeed = task()
    } catch (e: Exception) {
      logger.warn(e) { "[$this] attempt failed" }
    } finally {
      val nowAttempted = attempted + 1
      if (!AttemptedUpdater.compareAndSet(this, attempted, nowAttempted)) {
        val e = ConcurrentModificationException("should not happen")
        logger.error(e) { "[$this] is running concurrently" }
        throw e
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

  /**
   * 启动任务
   * @param runImmediately 是否立即执行一次
   */
  fun start(runImmediately: Boolean = false) {
    if (attempts == 0) {
      return
    }
    if (!AttemptedUpdater.compareAndSet(this, -1, 0)) {
      throw IllegalStateException("[$this] started.")
    }
    val delay = if (runImmediately) 0 else intervalMillis
    schedule(delay)
  }

  private fun schedule(delayMillis: Int) {
    ScheduledExecutor.schedule({ executor.execute(this) }, delayMillis.toLong(), TimeUnit.MILLISECONDS)
  }

  override fun toString(): String {
    return "Retry $name $attempts times every ${intervalMillis}ms"
  }

  companion object {
    private val logger = getLogger()

    @JvmStatic
    private val AttemptedUpdater = AtomicLongFieldUpdater.newUpdater(Retryable::class.java, "attempted")

    fun forever(
      name: String,
      intervalMillis: Int,
      executor: Executor = CommonPool,
      task: () -> Boolean
    ): Retryable = Retryable(name, -1, intervalMillis, executor, task)
  }
}
