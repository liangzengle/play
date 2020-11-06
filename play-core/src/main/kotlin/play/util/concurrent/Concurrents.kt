package play.util.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadFactory

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun threadFactory(
  name: String,
  isDaemon: Boolean = false,
  priority: Int = Thread.currentThread().priority
): ThreadFactory {
  return NamedThreadFactory(name, isDaemon, priority)
}

/**
 * @see java.util.concurrent.ForkJoinPool.commonPool
 */
object CommonPool : ExecutorService by ForkJoinPool.commonPool()
