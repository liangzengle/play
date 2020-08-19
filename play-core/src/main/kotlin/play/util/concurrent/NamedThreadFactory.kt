package play.util.concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

/**
 * @author LiangZengle
 */
class NamedThreadFactory constructor(
  private val namePrefix: String,
  private val isDaemon: Boolean,
  private val priority: Int,
  private val threadGroup: ThreadGroup?,
  private val exceptionHandler: Thread.UncaughtExceptionHandler?
) : ThreadFactory {

  @Suppress("unused")
  @Volatile
  private var count = 0

  override fun newThread(r: Runnable): Thread {
    val thread = Thread(threadGroup, r)
    thread.name = namePrefix + "-" + COUNTER.incrementAndGet(this)
    thread.isDaemon = isDaemon
    thread.priority = priority
    thread.uncaughtExceptionHandler = exceptionHandler
    return thread
  }

  companion object {
    @JvmStatic
    private val COUNTER =
      AtomicIntegerFieldUpdater.newUpdater(NamedThreadFactory::class.java, "count")

    @JvmStatic
    fun newBuilder(namePrefix: String) = Builder(namePrefix)
  }

  class Builder constructor(private val namePrefix: String) {
    private var daemon: Boolean = false
    private var priority: Int = Thread.currentThread().priority
    private var threadGroup: ThreadGroup? = null
    private var exceptionHandler: Thread.UncaughtExceptionHandler? = null

    fun daemon(isDaemon: Boolean): Builder {
      this.daemon = isDaemon
      return this
    }

    fun priority(priority: Int): Builder {
      this.priority = priority
      return this
    }

    fun threadGroup(threadGroup: ThreadGroup?): Builder {
      this.threadGroup = threadGroup
      return this
    }

    fun exceptionHandler(exceptionHandler: Thread.UncaughtExceptionHandler?): Builder {
      this.exceptionHandler = exceptionHandler
      return this
    }

    fun build(): NamedThreadFactory {
      return NamedThreadFactory(namePrefix, daemon, priority, threadGroup, exceptionHandler)
    }
  }
}

fun threadFactory(
  namePrefix: String,
  isDaemon: Boolean = false,
  priority: Int = Thread.currentThread().priority,
  threadGroup: ThreadGroup? = null,
  exceptionHandler: Thread.UncaughtExceptionHandler? = null
): ThreadFactory {
  return NamedThreadFactory(namePrefix, isDaemon, priority, threadGroup, exceptionHandler)
}
