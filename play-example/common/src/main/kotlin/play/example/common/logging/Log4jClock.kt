package play.example.common.logging

import org.apache.logging.log4j.core.util.Clock
import play.util.time.Time

/**
 * Clock for log timestamp
 *
 * @author LiangZengle
 */
class Log4jClock : Clock {
  override fun currentTimeMillis(): Long {
    return Time.currentMillis()
  }
}
