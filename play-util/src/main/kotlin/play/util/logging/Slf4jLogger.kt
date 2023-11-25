package play.util.logging

import org.slf4j.Logger
import play.util.logging.PlayLogger.Companion.safeEval

class Slf4jLogger(private val logger: Logger) : Logger by logger, PlayLogger {
  override fun trace(msg: () -> String) {
    if (isTraceEnabled) {
      logger.trace(safeEval(msg))
    }
  }

  override fun trace(cause: Throwable, msg: () -> String) {
    if (isTraceEnabled) {
      logger.trace(safeEval(msg), cause)
    }
  }

  override fun debug(msg: () -> String) {
    if (isDebugEnabled) {
      logger.debug(safeEval(msg))
    }
  }

  override fun debug(cause: Throwable, msg: () -> String) {
    if (isDebugEnabled) {
      logger.debug(safeEval(msg), cause)
    }
  }

  override fun info(msg: () -> String) {
    if (isInfoEnabled) {
      logger.info(safeEval(msg))
    }
  }

  override fun info(cause: Throwable, msg: () -> String) {
    if (isInfoEnabled) {
      logger.info(safeEval(msg), cause)
    }
  }

  override fun warn(msg: () -> String) {
    if (isWarnEnabled) {
      logger.warn(safeEval(msg))
    }
  }

  override fun warn(cause: Throwable, msg: () -> String) {
    if (isWarnEnabled) {
      logger.warn(safeEval(msg), cause)
    }
  }

  override fun error(msg: () -> String) {
    if (isErrorEnabled) {
      logger.error(safeEval(msg))
    }
  }

  override fun error(cause: Throwable, msg: () -> String) {
    if (isErrorEnabled) {
      logger.error(safeEval(msg), cause)
    }
  }
}
