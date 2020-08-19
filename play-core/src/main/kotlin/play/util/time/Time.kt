@file:JvmName("Time")
@file:Suppress("NOTHING_TO_INLINE")

package play.util.time

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.WeekFields
import java.util.*

@JvmField
val clock: Clock = Clock.systemDefaultZone()

fun timeZone(): TimeZone = TimeZone.getTimeZone(clock.zone)

fun currentDateTime(): LocalDateTime = LocalDateTime.now(clock)

fun currentDate(): LocalDate = LocalDate.now(clock)

fun currentTime(): LocalTime = LocalTime.now(clock)

fun currentMillis() = clock.millis()

fun currentSeconds(): Int = (clock.millis() / 1000).toInt()

inline fun nanoTime(): Long = System.nanoTime()

fun Clock.date(): LocalDate = LocalDate.now(this)

fun Clock.time(): LocalTime = LocalTime.now(this)

fun Clock.dateTime(): LocalDateTime = LocalDateTime.now(this)

fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), clock.zone)

fun LocalDateTime.toMillis() = this.atZone(clock.zone).toInstant().toEpochMilli()

fun LocalDateTime.toDate(): Date = Date.from(this.atZone(clock.zone).toInstant())

fun LocalDate.formatToInt(): Int = year * 10000 + month.value * 100 + dayOfMonth

fun LocalTime.formatToInt(): Int = hour * 10000 + minute * 100 + second

fun LocalDateTime.formatToLong(): Int = toLocalDate().formatToInt() * 1000000 + toLocalTime().formatToInt()

fun LocalDateTime.betweenMillis(toExclusive: LocalDateTime) = between(ChronoUnit.MILLIS, this, toExclusive)

fun LocalDateTime.betweenSeconds(toExclusive: LocalDateTime) = between(ChronoUnit.SECONDS, this, toExclusive)

fun LocalDateTime.betweenHours(toExclusive: LocalDateTime) = between(ChronoUnit.HOURS, this, toExclusive)

fun LocalDateTime.weekEquals(another: Temporal): Boolean {
  val field = WeekFields.of(DayOfWeek.MONDAY, 7)
  val weekOfWeekBasedYear = field.weekOfWeekBasedYear()
  val weekBasedYear = field.weekBasedYear()
  return this.get(weekOfWeekBasedYear) == another.get(weekOfWeekBasedYear) &&
    this.get(weekBasedYear) == another.get(weekBasedYear)
}

fun LocalDateTime.monthEquals(another: LocalDateTime): Boolean =
  this.year == another.year && this.month == another.month

fun LocalDateTime.monthEquals(another: LocalDate): Boolean = this.year == another.year && this.month == another.month

fun LocalDate.atEndOfDay(): LocalDateTime = LocalDateTime.of(this, LocalTime.MAX)

fun LocalDate.weekEquals(another: Temporal): Boolean {
  val field = WeekFields.of(DayOfWeek.MONDAY, 7).weekOfWeekBasedYear()
  return this.get(field) == another.get(field) && ChronoUnit.WEEKS.between(this, another) == 0L
}

fun LocalDate.monthEquals(another: LocalDateTime): Boolean = this.monthEquals(another.toLocalDate())

fun LocalDate.monthEquals(another: LocalDate): Boolean = this.year == another.year && this.month == another.month

fun LocalDate.betweenDays(toExclusive: LocalDate): Long = between(ChronoUnit.DAYS, this, toExclusive)

fun LocalDate.betweenWeeks(toExclusive: LocalDate): Long = between(ChronoUnit.WEEKS, this, toExclusive)

fun LocalDate.betweenMonths(toExclusive: LocalDate): Long = between(ChronoUnit.MONTHS, this, toExclusive)

fun Date.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this.toInstant(), clock.zone)

fun between(unit: ChronoUnit, fromInclusive: Temporal, toExclusive: Temporal): Long {
  return unit.between(fromInclusive, toExclusive)
}

fun isSameDay(t1: Long, t2: Long): Boolean =
  t1.toLocalDateTime().toLocalDate() == t2.toLocalDateTime().toLocalDate()

fun isSameWeek(t1: Long, t2: Long): Boolean = t1.toLocalDateTime().weekEquals(t2.toLocalDateTime())

fun isSameMonth(t1: Long, t2: Long): Boolean = t1.toLocalDateTime().monthEquals(t2.toLocalDateTime())

fun isToday(timeInMillis: Long): Boolean = timeInMillis.toLocalDateTime().toLocalDate() == currentDate()

fun isCurrentWeek(timeInMillis: Long): Boolean = isSameWeek(timeInMillis, currentMillis())

fun isCurrentMonth(timeInMillis: Long): Boolean = isSameMonth(timeInMillis, currentMillis())

/**
 * 判断两个时间段是否有重叠
 *
 * @param startTime1 时间段1的起始时间
 * @param endTime1 时间段1的结束时间
 * @param startTime2 时间段2的起始时间
 * @param endTime2 时间段2的结束时间
 * @return true-重叠
 */
fun isOverlap(
  startTime1: LocalDateTime,
  endTime1: LocalDateTime,
  startTime2: LocalDateTime,
  endTime2: LocalDateTime
): Boolean {
  val minEndTime = minOf(endTime1, endTime2)
  val maxFromTime = maxOf(startTime1, startTime2)
  return minEndTime > maxFromTime
}

/**
 * 1970-1-1 00:00:00
 */
@JvmField
val LongTimeAgo: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

/**
 * yyyy-MM-dd HH:mm:ss
 */
@JvmField
val yyyy_MM_dd_HH_mm_ss: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * yyyy-MM-ddTHH:mm:ss
 */
@JvmField
val yyyy_MM_ddTHH_mm_ss: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

/**
 * yyyyMMddHHmmss
 */
@JvmField
val yyyyMMddHHmmss: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

/**
 * yyyy-MM-dd
 */
@JvmField
val yyyy_MM_dd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * yyyyMMdd
 */
@JvmField
val yyyyMMdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * HH:mm:ss
 */
@JvmField
val HH_mm_ss: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * HHmmss
 */
@JvmField
val HHmmss: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")
