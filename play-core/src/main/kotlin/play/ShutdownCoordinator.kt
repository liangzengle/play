package play

import play.util.logging.LogCloser
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 应用关闭时按顺序执行关闭任务
 *
 * Created by LiangZengle on 2020/2/16.
 */
interface ShutdownCoordinator {

  /**
   * 添加一个关闭任务
   *
   * @param name 唯一的名称
   * @param priority 优先级(越小越高): [PRIORITY_HIGHEST]、[PRIORITY_NORMAL]、[PRIORITY_LOWEST]
   * @param task 要执行的任务
   */
  fun addShutdownTask(name: String, priority: Int = Order.Normal, task: () -> Unit)

  fun shutdown()

  companion object {
    const val PRIORITY_HIGHEST = Int.MIN_VALUE

    const val PRIORITY_NORMAL = 0

    const val PRIORITY_LOWEST = Int.MAX_VALUE
  }
}

class DefaultShutdownCoordinator : ShutdownCoordinator {
  private val stopped = AtomicBoolean()

  init {
    Runtime.getRuntime().addShutdownHook(thread(false) { shutdown() })
  }

  private fun isStopped() = stopped.get()

  override fun shutdown() {
    if (!stopped.compareAndSet(false, true)) {
      return
    }
    Log.info { "Application shutting down..." }
    synchronized(this) {
      val succeed = hooks.sorted().fold(true) { status, hook -> hook.run() && status }
      if (succeed) {
        Log.info { "Application shutdown successfully." }
      } else {
        Log.warn { "Application shutdown EXCEPTIONALLY!!!" }
      }
    }
    LogCloser.shutdown()
  }

  private val hooks = LinkedList<PriorityShutdownTask>()

  override fun addShutdownTask(name: String, priority: Int, task: () -> Unit) {
    require(name.isNotEmpty()) { "name is empty." }
    val hook = PriorityShutdownTask(name, priority, task)
    synchronized(this) {
      if (isStopped()) {
        hook.run()
      } else {
        hooks.add(hook)
      }
    }
  }

  private class PriorityShutdownTask(
    val name: String,
    val priority: Int,
    private val task: () -> Unit
  ) : Comparable<PriorityShutdownTask> {

    fun run(): Boolean {
      return try {
        Log.info { "$name..." }
        task()
        true
      } catch (e: Throwable) {
        Log.error(e) { "[$name]执行失败" }
        false
      }
    }

    override fun compareTo(other: PriorityShutdownTask): Int = priority.compareTo(other.priority)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as PriorityShutdownTask

      if (name != other.name) return false

      return true
    }

    override fun hashCode(): Int {
      return name.hashCode()
    }
  }
}
