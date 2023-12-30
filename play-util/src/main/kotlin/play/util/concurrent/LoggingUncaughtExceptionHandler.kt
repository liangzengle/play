package play.util.concurrent

import play.util.exception.isFatal
import play.util.logging.PlayLoggerManager

/**
 * @author LiangZengle
 */
object LoggingUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
  private val logger = PlayLoggerManager.getLogger(this.javaClass)

  override fun uncaughtException(t: Thread, e: Throwable) {
    if (e.isFatal()) {
      throw e
    }
    e.printStackTrace()
    logger.error(e) { "An exception has been raised by ${t.name}" }
  }
}
