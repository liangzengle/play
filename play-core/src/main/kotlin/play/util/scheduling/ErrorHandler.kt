package play.util.scheduling

import play.getLogger
import play.util.exception.isFatal

/**
 * Created by LiangZengle on 2020/2/20.
 */
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
