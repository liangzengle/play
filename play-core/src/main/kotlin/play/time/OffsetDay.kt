package play.time

import play.time.Time.betweenDays
import play.time.Time.toMillis
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.annotation.concurrent.Immutable
import kotlin.math.absoluteValue

/**
 * 自定义一天的0点时间
 * ```
 *  // 如5点开始算新的一天
 *  val offsetDay = OffsetDay(LocalTime.of(5, 0, 0))
 *  // following will return true
 *  offsetDay.isSameDay(
 *    LocalDateTime.of(2022,1,1,5,0,0),
 *    LocalDateTime.of(2022,1,2,4,0,0)
 *  )
 * ```
 * @author LiangZengle
 */
@Immutable
@Suppress("MemberVisibilityCanBePrivate")
class OffsetDay(val offset: LocalTime) {
  val offsetDuration: Duration = Duration.between(LocalTime.MIN, offset)

  /**
   * 判断两个日期是否为同一天
   *
   * @param t1 日期1
   * @param t2 日期2
   * @return 是否为同一天
   */
  fun isSameDay(t1: LocalDateTime, t2: LocalDateTime): Boolean {
    return t1.minus(offsetDuration).toLocalDate() == t2.minus(offsetDuration).toLocalDate()
  }

  /**
   * 判断两个日期是否为同一天
   *
   * @param t1 日期1的毫秒表示
   * @param t2 日期2的毫秒表示
   * @return 是否为同一天
   */
  fun isSameDay(t1: Long, t2: Long): Boolean {
    return isSameDay(Time.toLocalDateTime(t1), Time.toLocalDateTime(t2))
  }

  /**
   * 获取时间[t]的自定义0点时间
   * ```
   *  val offsetDay = OffsetDay(LocalTime.of(5, 0, 0))
   *  offsetDay.atStartOfDay(LocalDateTime.of(2022,1,1,10,0,0)) // returns: LocalDateTime.of(2022,1,1,5,0,0)
   *  offsetDay.atStartOfDay(LocalDateTime.of(2022,1,1,4,0,0)) // returns: LocalDateTime.of(2021,12,31,5,0,0)
   * ```
   * @param t 日期
   * @return
   */
  fun atStartOfDay(t: LocalDateTime): LocalDateTime {
    return LocalDateTime.of(t.minus(offsetDuration).toLocalDate(), offset)
  }

  /**
   * 获取时间[t]的自定义0点时间
   */
  fun atStartOfDay(t: Long): Long {
    return atStartOfDay(Time.toLocalDateTime(t)).toMillis()
  }

  /**
   * 计算从[t1]到[t2]的天数差异
   * ```
   *  val offsetDay = OffsetDay(LocalTime.of(5, 0, 0))
   *  offsetDay.diffDays(
   *    LocalDateTime.of(2022,1,1,5,0,0),
   *    LocalDateTime.of(2022,1,2,4,0,0)
   *  ) // returns: 0
   *
   *  offsetDay.diffDays(
   *    LocalDateTime.of(2022,1,1,4,0,0),
   *    LocalDateTime.of(2022,1,1,5,0,0)
   *  ) // returns: 1
   *
   * ```
   * @param t1 日期1
   * @param t2 日期2
   * @return 如果[t2]>[t1]，则返回负数
   */
  fun betweenDays(t1: LocalDateTime, t2: LocalDateTime): Long {
    return t1.minus(offsetDuration).toLocalDate().betweenDays(t2.minus(offsetDuration).toLocalDate())
  }

  /**
   * 计算从[t1]到[t2]的天数差异
   *
   * @param t1 日期1的毫秒表示
   * @param t2 日期2的毫秒表示
   * @return 如果[t2]>[t1]，则返回负数
   */
  fun betweenDays(t1: Long, t2: Long): Long {
    return betweenDays(Time.toLocalDateTime(t1), Time.toLocalDateTime(t2))
  }

  /**
   * 计算两个时间的差异天数
   * ```
   * 等价于:
   *   Math.abs(betweenDays(t1, t2))
   * ```
   * @param t1 日期1
   * @param t2 日期2
   * @return 非负数
   */
  fun diffDays(t1: LocalDateTime, t2: LocalDateTime): Long {
    return betweenDays(t1, t2).absoluteValue
  }

  /**
   * 计算两个时间的差异天数
   * ```
   * 等价于:
   *   Math.abs(betweenDays(t1, t2))
   * ```
   * @param t1 日期1的毫秒表示
   * @param t2 日期2的毫秒表示
   * @return 非负数
   */
  fun diffDays(t1: Long, t2: Long): Long {
    return betweenDays(t1, t2).absoluteValue
  }
}
