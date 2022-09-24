package play.util.time

import play.util.primitive.toIntChecked
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.time.*
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.WeekFields
import java.util.*
import java.util.regex.Pattern

object Time {

  /**
   * 1970-1-1 00:00:00
   */
  @JvmStatic
  val LongTimeAgo: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

  @JvmStatic
  private val SimpleDurationPattern = Pattern.compile("^([+-]?\\d+)([a-zA-Z]{0,2})$")

  @JvmStatic
  private val RepeatedSimpleDurationPattern = Pattern.compile("([+-]?\\d+)([a-zA-Z]{0,2})")

  @JvmStatic
  private val StandardDurationPattern = Pattern.compile("^[+-]?P.*$")

  private val clock = DelegatingClock(Clock.systemDefaultZone())

  private val pcs = PropertyChangeSupport(this)

  @JvmStatic
  fun clock(): Clock = clock

  @JvmStatic
  fun setClock(newClock: Clock) {
    val prev = clock.underlying
    clock.underlying = newClock
    pcs.firePropertyChange("clock", prev, newClock)
  }

  @JvmStatic
  fun setClockOffset(offsetDuration: Duration) {
    setClock(Clock.offset(clock.underlying, offsetDuration))
  }

  @JvmStatic
  fun addClockChangeListener(listener: PropertyChangeListener) {
    pcs.addPropertyChangeListener(listener)
  }

  @JvmStatic
  fun removeClockChangeListener(listener: PropertyChangeListener) {
    pcs.removePropertyChangeListener(listener)
  }

  @JvmStatic
  fun timeZone(): TimeZone = TimeZone.getTimeZone(clock.zone)

  @JvmStatic
  fun currentDateTime(): LocalDateTime = LocalDateTime.now(clock)

  @JvmStatic
  fun currentDate(): LocalDate = LocalDate.now(clock)

  @JvmStatic
  fun currentTime(): LocalTime = LocalTime.now(clock)

  @JvmStatic
  fun currentMillis() = clock.millis()

  @JvmStatic
  fun currentSecondsInt(): Int = (clock.millis() / 1000).toIntChecked()

  @JvmStatic
  fun currentSeconds(): Long = clock.millis() / 1000

  @JvmStatic
  fun nanoTime(): Long = System.nanoTime()

  @JvmStatic
  fun toLocalDateTime(timeInMillis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), clock.zone)

  @JvmStatic
  fun LocalDateTime.toMillis() = this.atZone(clock.zone).toInstant().toEpochMilli()

  @JvmStatic
  fun LocalDateTime.toDate(): Date = Date.from(this.atZone(clock.zone).toInstant())

  @JvmStatic
  fun LocalDate.formatToInt(): Int = year * 10000 + month.value * 100 + dayOfMonth

  @JvmStatic
  fun LocalTime.formatToInt(): Int = hour * 10000 + minute * 100 + second

  @JvmStatic
  fun LocalDateTime.formatToLong(): Long = toLocalDate().formatToInt() * 1000000L + toLocalTime().formatToInt()

  @JvmStatic
  fun LocalDateTime.betweenMillis(toExclusive: LocalDateTime) = between(ChronoUnit.MILLIS, this, toExclusive)

  @JvmStatic
  fun LocalDateTime.betweenSeconds(toExclusive: LocalDateTime) = between(ChronoUnit.SECONDS, this, toExclusive)

  @JvmStatic
  fun LocalDateTime.betweenHours(toExclusive: LocalDateTime) = between(ChronoUnit.HOURS, this, toExclusive)

  @JvmStatic
  fun LocalDateTime.weekEquals(another: Temporal): Boolean {
    val field = WeekFields.of(DayOfWeek.MONDAY, 7)
    val weekOfWeekBasedYear = field.weekOfWeekBasedYear()
    val weekBasedYear = field.weekBasedYear()
    return this.get(weekOfWeekBasedYear) == another.get(weekOfWeekBasedYear) &&
      this.get(weekBasedYear) == another.get(weekBasedYear)
  }

  @JvmStatic
  fun LocalDateTime.monthEquals(another: LocalDateTime): Boolean =
    this.year == another.year && this.month == another.month

  @JvmStatic
  fun LocalDateTime.monthEquals(another: LocalDate): Boolean = this.year == another.year && this.month == another.month

  @JvmStatic
  fun LocalDate.atEndOfDay(): LocalDateTime = LocalDateTime.of(this, LocalTime.MAX)

  @JvmStatic
  fun LocalDate.weekEquals(another: Temporal): Boolean {
    val field = WeekFields.of(DayOfWeek.MONDAY, 7).weekOfWeekBasedYear()
    return this.get(field) == another.get(field) && ChronoUnit.WEEKS.between(this, another) == 0L
  }

  @JvmStatic
  fun LocalDate.monthEquals(another: LocalDateTime): Boolean = this.monthEquals(another.toLocalDate())

  @JvmStatic
  fun LocalDate.monthEquals(another: LocalDate): Boolean = this.year == another.year && this.month == another.month

  @JvmStatic
  fun LocalDate.betweenDays(toExclusive: LocalDate): Long = between(ChronoUnit.DAYS, this, toExclusive)

  @JvmStatic
  fun LocalDate.betweenWeeks(toExclusive: LocalDate): Long = between(ChronoUnit.WEEKS, this, toExclusive)

  @JvmStatic
  fun LocalDate.betweenMonths(toExclusive: LocalDate): Long = between(ChronoUnit.MONTHS, this, toExclusive)

  @JvmStatic
  fun Date.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this.toInstant(), clock.zone)

  @JvmStatic
  fun between(unit: ChronoUnit, fromInclusive: Temporal, toExclusive: Temporal): Long {
    return unit.between(fromInclusive, toExclusive)
  }

  @JvmStatic
  fun isSameDay(t1: Long, t2: Long): Boolean = toLocalDateTime(t1).toLocalDate() == toLocalDateTime(t2).toLocalDate()

  @JvmStatic
  fun isSameWeek(t1: Long, t2: Long): Boolean = toLocalDateTime(t1).weekEquals(toLocalDateTime(t2))

  @JvmStatic
  fun isSameMonth(t1: Long, t2: Long): Boolean = toLocalDateTime(t1).monthEquals(toLocalDateTime(t2))

  @JvmStatic
  fun isToday(timeInMillis: Long): Boolean = toLocalDateTime(timeInMillis).toLocalDate() == currentDate()

  @JvmStatic
  fun isCurrentWeek(timeInMillis: Long): Boolean = isSameWeek(timeInMillis, currentMillis())

  @JvmStatic
  fun isCurrentMonth(timeInMillis: Long): Boolean = isSameMonth(timeInMillis, currentMillis())

  @JvmStatic
  fun LocalDate.atDayOfWeek(dayOfWeek: DayOfWeek): LocalDate =
    this.with(ChronoField.DAY_OF_WEEK, dayOfWeek.value.toLong())

  @JvmStatic
  fun LocalDate.atEndOfWeek(): LocalDate = this.atDayOfWeek(DayOfWeek.SUNDAY)

  @JvmStatic
  fun LocalDate.atEndOfMonth(): LocalDate = this.withDayOfMonth(this.lengthOfMonth())

  /**
   * 判断两个时间段是否有重叠
   *
   * @param startTime1 时间段1的起始时间
   * @param endTime1 时间段1的结束时间
   * @param startTime2 时间段2的起始时间
   * @param endTime2 时间段2的结束时间
   * @return true-重叠
   */
  @JvmStatic
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

  @Throws(DateTimeParseException::class)
  @JvmStatic
  fun parseDuration(durationText: String): Duration {
    // match: 100 or 1m
    try {
      var matcher = SimpleDurationPattern.matcher(durationText)
      if (matcher.matches()) {
        val length = matcher.group(1).toLong()
        val unit = parseUnit(matcher.group(2), ChronoUnit.MILLIS)
        return Duration.of(length, unit)
      }
      // match: P1D
      matcher = StandardDurationPattern.matcher(durationText)
      if (matcher.matches()) {
        return Duration.parse(durationText)
      }
      // match: 1h1m
      matcher = RepeatedSimpleDurationPattern.matcher(durationText)
      var duration = Duration.ZERO
      var hasNext = matcher.find()
      while (hasNext) {
        val length = matcher.group(1).toLong()
        val unit = parseUnit(matcher.group(2), ChronoUnit.MILLIS)
        duration = duration.plus(Duration.of(length, unit))
        hasNext = matcher.find()
        if (!hasNext) {
          return duration
        }
      }
    } catch (e: DateTimeParseException) {
      throw e
    } catch (e: Throwable) {
      throw DateTimeParseException("Text cannot be parsed to a Duration: $durationText", durationText, 0).initCause(e)
    }
    throw DateTimeParseException("Text cannot be parsed to a Duration: $durationText", durationText, 0)
  }

  @JvmStatic
  fun parseUnit(unit: String?, ifNullOrEmpty: ChronoUnit): ChronoUnit {
    return if (unit.isNullOrEmpty()) ifNullOrEmpty else parseUnit(unit)
  }

  @Throws(IllegalArgumentException::class)
  @JvmStatic
  fun parseUnit(unit: String): ChronoUnit {
    return when (unit.lowercase()) {
      "ms" -> ChronoUnit.MILLIS
      "s" -> ChronoUnit.SECONDS
      "m" -> ChronoUnit.MINUTES
      "h" -> ChronoUnit.HOURS
      "d" -> ChronoUnit.DAYS
      "ns" -> ChronoUnit.NANOS
      "us" -> ChronoUnit.MICROS
      else -> throw IllegalArgumentException("Unknown unit: $unit")
    }
  }
}
