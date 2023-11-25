package play.util.logging

import play.util.exception.isFatal

interface PlayLogger {

  companion object {
    context(PlayLogger)
    fun safeEval(msg: () -> String): String {
      return try {
        msg.invoke()
      } catch (e: Throwable) {
        if (e.isFatal()) {
          throw e
        }
        error(e) { "Failed to evaluate log message" }
        "Log message invocation failed: $e"
      }
    }


  }

  fun getName(): String
  fun isTraceEnabled(): Boolean

  fun isDebugEnabled(): Boolean

  fun isInfoEnabled(): Boolean

  fun isWarnEnabled(): Boolean

  fun isErrorEnabled(): Boolean

  fun trace(msg: () -> String)

  fun trace(cause: Throwable, msg: () -> String)

  fun debug(msg: () -> String)

  fun debug(cause: Throwable, msg: () -> String)

  fun info(msg: () -> String)

  fun info(cause: Throwable, msg: () -> String)

  fun warn(msg: () -> String)

  fun warn(cause: Throwable, msg: () -> String)

  fun error(msg: () -> String)

  fun error(cause: Throwable, msg: () -> String)
}
