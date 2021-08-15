package play.example.common.akka.scheduling

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * millis based scheduler
 * @author LiangZengle
 */
class LightArrayRevolverScheduler(config: Config, log: LoggingAdapter, threadFactory: ThreadFactory) :
  akka.actor.LightArrayRevolverScheduler(config, log, threadFactory) {

  override fun clock(): Long {
    return  TimeUnit.MILLISECONDS.toNanos(play.util.time.clock.millis())
  }
}
