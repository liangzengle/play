package play.util.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class DelegatingClock(@JvmField var underlying: Clock) : Clock() {

  override fun instant(): Instant = underlying.instant()

  override fun withZone(zone: ZoneId): Clock = underlying.withZone(zone)

  override fun getZone(): ZoneId = underlying.zone

  override fun millis(): Long = underlying.millis()

  override fun toString(): String {
    return underlying.toString()
  }
}

