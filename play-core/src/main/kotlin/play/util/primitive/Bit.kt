package play.util.primitive

import org.checkerframework.common.value.qual.IntRange
import javax.annotation.CheckReturnValue

/**
 * 位操作，位从1开始
 *
 * @author LiangZengle
 */
object Bit {

  /**
   * 将[value]的第[bit]位置为1
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun set(value: Int, bit: @IntRange(from = 1, to = 32) Int): Int {
    return value or (1 shl (bit - 1))
  }

  /**
   * 将[value]的第[bit]位置为0
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun clear(value: Int, bit: @IntRange(from = 1, to = 32) Int): Int {
    return value and (1 shl (bit - 1)).inv()
  }

  /**
   * 判断[value]的第[bit]位是否为1
   *
   * @param value 值
   * @param bit 第几位，从1开始
   * @return 是否为1
   */
  @JvmStatic
  fun is1(value: Int, bit: @IntRange(from = 1, to = 32) Int): Boolean {
    return (value and (1 shl (bit - 1))) != 0
  }

  /**
   * 判断[value]的第[bit]位是否为0
   *
   * @param value 值
   * @param bit 第几位，从1开始
   * @return 是否为0
   */
  @JvmStatic
  fun is0(value: Int, bit: @IntRange(from = 1, to = 32) Int): Boolean {
    return (value and (1 shl (bit - 1))) == 0
  }

  /**
   * 将[value]的第[bit]位置为1
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun set(value: Long, bit: @IntRange(from = 1, to = 64) Int): Long {
    return value or (1.toLong() shl (bit - 1))
  }

  /**
   * 将[value]的第[bit]位置为0
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun clear(value: Long, bit: @IntRange(from = 1, to = 64) Int): Long {
    return value and (1.toLong() shl (bit - 1)).inv()
  }

  /**
   * 判断[value]的第[bit]位是否为1
   *
   * @param value 值
   * @param bit 第几位，从1开始
   * @return 是否为1
   */
  @JvmStatic
  fun is1(value: Long, bit: @IntRange(from = 1, to = 64) Int): Boolean {
    return (value and (1L shl (bit - 1))) != 0L
  }

  /**
   * 判断[value]的第[bit]位是否为0
   *
   * @param value 值
   * @param bit 第几位，从1开始
   * @return 是否为0
   */
  @JvmStatic
  fun is0(value: Long, bit: @IntRange(from = 1, to = 64) Int): Boolean {
    return (value and (1L shl (bit - 1))) == 0L
  }
}
