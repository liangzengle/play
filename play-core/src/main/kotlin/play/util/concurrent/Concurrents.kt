package play.util.concurrent

import io.vavr.concurrent.Future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun threadFactory(
  name: String,
  isDaemon: Boolean = false,
  priority: Int = Thread.currentThread().priority
): ThreadFactory {
  return object : ThreadFactory {
    private val counter = AtomicInteger()

    override fun newThread(r: Runnable): Thread {
      val thread = Thread(r)
      thread.name = name + "-" + counter.incrementAndGet()
      thread.isDaemon = isDaemon
      thread.priority = priority
      return thread
    }
  }
}

/**
 * @see java.util.concurrent.ForkJoinPool.commonPool
 */
object CommonPool : ExecutorService by ForkJoinPool.commonPool()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> CompletableFuture<T>.toFuture() = Future.fromCompletableFuture(this)!!
