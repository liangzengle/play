package play.util.logging

import org.slf4j.LoggerFactory
import org.slf4j.spi.LocationAwareLogger

object SlfLoggerFactory : PlayLoggerFactory {
  override fun getLogger(name: String): PlayLogger {
    val logger = LoggerFactory.getLogger(name)
    return if (logger is LocationAwareLogger) LocationAwareSlf4jLogger(logger) else Slf4jLogger(logger)
  }
}
