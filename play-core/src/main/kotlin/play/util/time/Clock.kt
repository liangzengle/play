package play.util.time

import play.util.primitive.toIntChecked
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

fun Clock.timeZone(): TimeZone = TimeZone.getTimeZone(zone)

fun Clock.currentDateTime(): LocalDateTime = LocalDateTime.now(this)

fun Clock.currentDate(): LocalDate = LocalDate.now(this)

fun Clock.currentTime(): LocalTime = LocalTime.now(this)

fun Clock.currentMillis() = this.millis()

@Deprecated("Use currentSeconds() instead", ReplaceWith("currentSeconds()"))
fun Clock.currentSecondsInt(): Int = (this.millis() / 1000).toIntChecked()

fun Clock.currentSeconds(): Long = this.millis() / 1000
