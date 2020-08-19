package play

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Created by LiangZengle on 2020/2/16.
 */
interface ApplicationLifecycle {

  fun stop()

  fun addShutdownHook(name: String, priority: Int = 0, hook: () -> Unit)

  companion object {
    const val PRIORITY_HIGHEST = Int.MIN_VALUE

    const val PRIORITY_NORMAL = 0

    const val PRIORITY_LOWEST = Int.MAX_VALUE
  }
}

class DefaultApplicationLifecycle : ApplicationLifecycle {
  private val stopped = AtomicBoolean()

  init {
    Runtime.getRuntime().addShutdownHook(thread(false) { stop() })
  }

  private fun isStopped() = stopped.get()

  override fun stop() {
    if (!stopped.compareAndSet(false, true)) {
      return
    }
    Log.info { "Application shutting down..." }
    synchronized(this) {
      val succeed = hooks.sorted().fold(true) { status, hook -> hook.run() && status }
      if (succeed) {
        Log.info { "Application shutdown successfully!" }
      } else {
        Log.warn { "Application shutdown EXCEPTIONALLY!" }
      }
    }
  }

  private val hooks = LinkedList<PriorityShutdownHook>()

  /**
   * @param name 唯一的名称
   * @param priority 优先级(越小越高)
   * @param hook 要执行的任务
   */
  override fun addShutdownHook(name: String, priority: Int, hook: () -> Unit) {
    if (isStopped()) {
      hook()
      return
    }
    synchronized(this) {
      hooks.add(PriorityShutdownHook(name, priority, hook))
    }
  }

  private class PriorityShutdownHook(
    val name: String,
    val priority: Int,
    private val task: () -> Unit
  ) :
    Comparable<PriorityShutdownHook> {
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

    override fun compareTo(other: PriorityShutdownHook): Int = priority.compareTo(other.priority)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as PriorityShutdownHook

      if (name != other.name) return false

      return true
    }

    override fun hashCode(): Int {
      return name.hashCode()
    }
  }
}
