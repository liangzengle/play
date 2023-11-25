@file:JvmName("Clocks")

package play.util.time

import play.util.primitive.toIntChecked
import java.time.*
import java.util.*

fun Clock.timeZone(): TimeZone = TimeZone.getTimeZone(zone)

fun Clock.currentDateTime(): LocalDateTime = LocalDateTime.now(this)

fun Clock.currentDate(): LocalDate = LocalDate.now(this)

fun Clock.currentTime(): LocalTime = LocalTime.now(this)

fun Clock.currentMillis() = this.millis()

@Deprecated("Use currentSeconds() instead", ReplaceWith("currentSeconds()"))
fun Clock.currentSecondsInt(): Int = (this.millis() / 1000).toIntChecked()

fun Clock.currentSeconds(): Long = this.millis() / 1000

fun Clock.instantNotNull(): Instant = this.instant()
