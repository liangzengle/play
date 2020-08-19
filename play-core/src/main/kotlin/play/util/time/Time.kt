package play.util.time

import play.util.primitive.toIntChecked
import java.time.*
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.WeekFields
import java.util.*

object Time {
  private val defaultClock = Clock.systemDefaultZone()

  var clock: Clock = defaultClock
    private set

  @Synchronized
  fun setClock(clock: Clock) {
    check(this.clock === defaultClock) { "clock has been set to ${this.clock}" }
    this.clock = clock
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
  fun LocalDateTime.formatToLong(): Int = toLocalDate().formatToInt() * 1000000 + toLocalTime().formatToInt()

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
  fun isSameDay(t1: Long, t2: Long): Boolean =
    toLocalDateTime(t1).toLocalDate() == toLocalDateTime(t2).toLocalDate()

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

  /**
   * 1970-1-1 00:00:00
   */
  @JvmStatic
  val LongTimeAgo: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
}
