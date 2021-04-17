package play.example.common.akka.scheduling

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import play.util.time.currentMillis
import java.util.concurrent.ThreadFactory

/**
 * millis based scheduler
 * @author LiangZengle
 */
class LightArrayRevolverScheduler(config: Config, log: LoggingAdapter, threadFactory: ThreadFactory) :
  akka.actor.LightArrayRevolverScheduler(config, log, threadFactory) {

  override fun clock(): Long = currentMillis()
}
