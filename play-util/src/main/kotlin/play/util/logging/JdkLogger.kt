package play.util.logging

import play.util.logging.PlayLogger.Companion.safeEval

class JdkLogger(private val logger: System.Logger) : System.Logger by logger, PlayLogger {

  override fun isTraceEnabled(): Boolean {
    return logger.isLoggable(System.Logger.Level.TRACE)
  }

  override fun isDebugEnabled(): Boolean {
    return logger.isLoggable(System.Logger.Level.DEBUG)
  }

  override fun isInfoEnabled(): Boolean {
    return logger.isLoggable(System.Logger.Level.INFO)
  }

  override fun isWarnEnabled(): Boolean {
    return logger.isLoggable(System.Logger.Level.WARNING)
  }

  override fun isErrorEnabled(): Boolean {
    return logger.isLoggable(System.Logger.Level.ERROR)
  }

  override fun trace(msg: () -> String) {
    if (isTraceEnabled()) {
      logger.log(System.Logger.Level.TRACE, safeEval(msg))
    }
  }

  override fun trace(cause: Throwable, msg: () -> String) {
    if (isTraceEnabled()) {
      logger.log(System.Logger.Level.TRACE, safeEval(msg), cause)
    }
  }

  override fun debug(msg: () -> String) {
    if (isDebugEnabled()) {
      logger.log(System.Logger.Level.DEBUG, safeEval(msg))
    }
  }

  override fun debug(cause: Throwable, msg: () -> String) {
    if (isDebugEnabled()) {
      logger.log(System.Logger.Level.DEBUG, safeEval(msg), cause)
    }
  }

  override fun info(msg: () -> String) {
    logger.log(System.Logger.Level.INFO, safeEval(msg))
  }

  override fun info(cause: Throwable, msg: () -> String) {
    if (isInfoEnabled()) {
      logger.log(System.Logger.Level.INFO, safeEval(msg), cause)
    }
  }

  override fun warn(msg: () -> String) {
    logger.log(System.Logger.Level.WARNING, safeEval(msg))
  }

  override fun warn(cause: Throwable, msg: () -> String) {
    if (isWarnEnabled()) {
      logger.log(System.Logger.Level.WARNING, safeEval(msg), cause)
    }
  }

  override fun error(msg: () -> String) {
    if (isErrorEnabled()) {
      logger.log(System.Logger.Level.ERROR, safeEval(msg))
    }
  }

  override fun error(cause: Throwable, msg: () -> String) {
    if (isErrorEnabled()) {
      logger.log(System.Logger.Level.ERROR, safeEval(msg), cause)
    }
  }
}
