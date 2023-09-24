package play.time

import java.time.format.DateTimeFormatter

object DateTimeFormatters {
  /**
   * yyyy-MM-dd HH:mm:ss
   */
  @JvmStatic
  val yyyy_MM_dd_HH_mm_ss: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  /**
   * yyyy-MM-ddTHH:mm:ss
   */
  @JvmStatic
  val yyyy_MM_ddTHH_mm_ss: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  /**
   * yyyyMMddHHmmss
   */
  @JvmStatic
  val yyyyMMddHHmmss: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  /**
   * yyyy-MM-dd
   */
  @JvmStatic
  val yyyy_MM_dd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
   * yyyyMMdd
   */
  @JvmStatic
  val yyyyMMdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  /**
   * HH:mm:ss
   */
  @JvmStatic
  val HH_mm_ss: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

  /**
   * HHmmss
   */
  @JvmStatic
  val HHmmss: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")
}
