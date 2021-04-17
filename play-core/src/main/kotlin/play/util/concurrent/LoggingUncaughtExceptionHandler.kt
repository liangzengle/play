package play.util.concurrent

import play.Log

/**
 * @author LiangZengle
 */
object LoggingUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(t: Thread, e: Throwable) {
    Log.error(e) { "An exception has been raised by ${t.name}" }
  }
}
