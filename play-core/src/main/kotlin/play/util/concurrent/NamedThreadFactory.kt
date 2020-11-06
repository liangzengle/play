package play.util.concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 * @author LiangZengle
 */
class NamedThreadFactory(
  private val namePrefix: String,
  private val isDaemon: Boolean = false,
  private val priority: Int = Thread.currentThread().priority,
  private val threadGroup: ThreadGroup? = null
) : ThreadFactory {

  @Suppress("unused")
  @Volatile
  private var count = 0

  override fun newThread(r: Runnable): Thread {
    val thread = Thread(threadGroup, r)
    thread.name = namePrefix + "-" + COUNTER.incrementAndGet(this)
    thread.isDaemon = isDaemon
    thread.priority = priority
    return thread
  }

  companion object {
    @JvmStatic
    private val COUNTER =
      AtomicIntegerFieldUpdater.newUpdater(NamedThreadFactory::class.java, "count")
  }
}
