package play.util.logging

import org.slf4j.LoggerFactory

object SlfLoggerFactory : PlayLoggerFactory {
  override fun getLogger(name: String): PlayLogger {
    return Slf4jLogger(LoggerFactory.getLogger(name))
  }
}
