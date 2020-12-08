package play.util.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadFactory

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun threadFactory(
  namePrefix: String,
  isDaemon: Boolean = false,
  priority: Int = Thread.currentThread().priority,
  threadGroup: ThreadGroup? = null,
  exceptionHandler: Thread.UncaughtExceptionHandler? = null,
): ThreadFactory {
  return NamedThreadFactory
    .newBuilder(namePrefix)
    .daemon(isDaemon)
    .priority(priority)
    .threadGroup(threadGroup)
    .exceptionHandler(exceptionHandler)
    .build()
}

/**
 * @see java.util.concurrent.ForkJoinPool.commonPool
 */
object CommonPool : ExecutorService by ForkJoinPool.commonPool()
