package play.util.scheduling

import play.getLogger
import play.util.exception.isFatal

interface ErrorHandler {
  fun handleError(t: Throwable)
}

object LoggingErrorHandler : ErrorHandler {
  private val logger = getLogger()

  override fun handleError(t: Throwable) {
    logger.error(t) { t.message }
    if (t.isFatal()) throw t
  }
}
