package play

import play.util.logging.LogCloser
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
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
   * @param ref 执行task的对象
   * @param task 要执行的任务，注意不要引用外部的变量
   */
  fun <T> addShutdownTask(name: String, priority: Int, ref: T, task: (T) -> Unit)

  /**
   * 添加一个关闭任务
   *
   * @param name 唯一的名称
   * @param ref 执行task的对象
   * @param task 要执行的任务，注意不要引用外部的变量
   */
  fun <T> addShutdownTask(name: String, ref: T, task: (T) -> Unit) {
    addShutdownTask(name, Orders.Normal, ref, task)
  }

  fun shutdown()

  companion object {
    const val PRIORITY_HIGHEST = Int.MIN_VALUE

    const val PRIORITY_NORMAL = 0

    const val PRIORITY_LOWEST = Int.MAX_VALUE
  }
}

class DefaultShutdownCoordinator : ShutdownCoordinator {
  @Suppress("unused")
  @Volatile
  private var stopped = 0

  companion object {
    @JvmStatic
    private val stoppedUpdater = AtomicIntegerFieldUpdater.newUpdater(DefaultShutdownCoordinator::class.java, "stopped")
  }

  init {
    Runtime.getRuntime().addShutdownHook(thread(false) { shutdown() })
  }

  private fun isStopped() = stoppedUpdater.get(this) == 1

  override fun shutdown() {
    if (!stoppedUpdater.compareAndSet(this, 0, 1)) {
      return
    }
    Log.info { "Application shutting down..." }
    synchronized(this) {
      val succeed = hooks.sorted().fold(true) { status, hook -> hook.run() && status }
      if (succeed) {
        Log.info { "Application shutdown successfully." }
      } else {
        Log.error { "Application shutdown EXCEPTIONALLY!!!" }
      }
    }
    LogCloser.shutdown()
  }

  private val hooks = LinkedList<PriorityShutdownTask<*>>()

  override fun <T> addShutdownTask(name: String, priority: Int, ref: T, task: (T) -> Unit) {
    require(name.isNotEmpty()) { "name is empty." }
    val hook = PriorityShutdownTask(name, priority, ref, task)
    synchronized(this) {
      if (isStopped()) {
        hook.run()
      } else {
        hooks.add(hook)
      }
    }
  }

  private class PriorityShutdownTask<T>(
    val name: String,
    val priority: Int,
    ref: T,
    private val task: (T) -> Unit
  ) : WeakReference<T>(ref), Comparable<PriorityShutdownTask<*>> {

    fun run(): Boolean {
      return try {
        Log.info { "$name..." }
        get()?.also(task)
        true
      } catch (e: Throwable) {
        Log.error(e) { "[$name]执行失败" }
        false
      }
    }

    override fun compareTo(other: PriorityShutdownTask<*>): Int = priority.compareTo(other.priority)

    override fun toString(): String {
      return "PriorityShutdownTask($name, $priority)"
    }
  }
}
