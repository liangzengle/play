package play.example.common.akka.scheduling

import akka.event.LoggingAdapter
import com.typesafe.config.Config
import java.time.Clock
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * millis based scheduler
 * @author LiangZengle
 */
class LightArrayRevolverScheduler(config: Config, log: LoggingAdapter, threadFactory: ThreadFactory) :
  akka.actor.LightArrayRevolverScheduler(config, log, threadFactory) {

  private var _clock: Clock? = null

  fun setClock(clock: Clock) {
    this._clock = clock
  }

  override fun clock(): Long {
    val clock = _clock ?: return super.clock()
    return TimeUnit.MILLISECONDS.toNanos(clock.millis())
  }
}
