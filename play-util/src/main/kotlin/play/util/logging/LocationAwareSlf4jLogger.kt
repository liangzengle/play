package play.util.logging

import org.slf4j.event.EventConstants
import org.slf4j.spi.LocationAwareLogger
import play.util.logging.PlayLogger.Companion.safeEval

class LocationAwareSlf4jLogger(private val logger: LocationAwareLogger) : LocationAwareLogger by logger, PlayLogger {

  companion object {
    @JvmStatic
    private val FQCN = LocationAwareSlf4jLogger::class.java.name
  }

  override fun trace(msg: () -> String) {
    if (isTraceEnabled) {
      log(null, FQCN, EventConstants.TRACE_INT, safeEval(msg), null, null)
    }
  }

  override fun trace(cause: Throwable, msg: () -> String) {
    if (isTraceEnabled) {
      log(null, FQCN, EventConstants.TRACE_INT, safeEval(msg), null, cause)
    }
  }

  override fun debug(msg: () -> String) {
    if (isDebugEnabled) {
      log(null, FQCN, EventConstants.DEBUG_INT, safeEval(msg), null, null)
    }
  }

  override fun debug(cause: Throwable, msg: () -> String) {
    if (isDebugEnabled) {
      log(null, FQCN, EventConstants.DEBUG_INT, safeEval(msg), null, cause)
    }
  }

  override fun info(msg: () -> String) {
    if (isInfoEnabled) {
      log(null, FQCN, EventConstants.INFO_INT, safeEval(msg), null, null)
    }
  }

  override fun info(cause: Throwable, msg: () -> String) {
    if (isInfoEnabled) {
      log(null, FQCN, EventConstants.INFO_INT, safeEval(msg), null, cause)
    }
  }

  override fun warn(msg: () -> String) {
    if (isWarnEnabled) {
      log(null, FQCN, EventConstants.WARN_INT, safeEval(msg), null, null)
    }
  }

  override fun warn(cause: Throwable, msg: () -> String) {
    if (isWarnEnabled) {
      log(null, FQCN, EventConstants.WARN_INT, safeEval(msg), null, cause)
    }
  }

  override fun error(msg: () -> String) {
    if (isErrorEnabled) {
      log(null, FQCN, EventConstants.ERROR_INT, safeEval(msg), null, null)
    }
  }

  override fun error(cause: Throwable, msg: () -> String) {
    if (isErrorEnabled) {
      log(null, FQCN, EventConstants.ERROR_INT, safeEval(msg), null, cause)
    }
  }
}
