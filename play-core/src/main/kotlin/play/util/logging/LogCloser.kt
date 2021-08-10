package play.util.logging

import ch.qos.logback.classic.LoggerContext
import org.apache.logging.log4j.LogManager
import org.slf4j.LoggerFactory

/**
 * @author LiangZengle
 */
object LogCloser {
  @JvmStatic
  fun shutdown() {
    try {
      LogManager.shutdown()
    } catch (e: NoClassDefFoundError) {
      try {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.stop()
      } catch (ex: NoClassDefFoundError) {
        // ignore
      }
    }
  }
}
