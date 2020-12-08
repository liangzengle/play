package play.util.scheduling.executor

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import play.Log
import play.util.concurrent.LoggingUncaughtExceptionHandler
import play.util.concurrent.NamedThreadFactory.Companion.newBuilder


/**
 * 内部用的定时器
 *
 * @author LiangZengle
 */
internal object ScheduledExecutor : ScheduledExecutorService by make()

private fun make(): ScheduledExecutorService {
  val threadFactory = newBuilder("scheduled-executor")
    .daemon(true)
    .exceptionHandler(LoggingUncaughtExceptionHandler)
    .build()
  return object : MillisBasedScheduledThreadPoolExecutor(
    1,
    10000,
    threadFactory
  ) {
    override fun afterExecute(r: Runnable?, t: Throwable?) {
      super.afterExecute(r, t)
      var ex = t
      if (ex == null && r is Future<*>) {
        try {
          if (r.isDone) r.get()
        } catch (e: CancellationException) {
          ex = e
        } catch (e: ExecutionException) {
          ex = e.cause
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }
      if (ex != null) Log.error(ex) { "Exception occurred when running $r" }
    }
  }
}
