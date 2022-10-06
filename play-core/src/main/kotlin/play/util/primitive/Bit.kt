package play.util.primitive

import org.checkerframework.checker.index.qual.Positive
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
  fun set1(value: Int, bit: @IntRange(from = 1, to = 32) Int): Int {
    require(bit in 1..32) { "bit must in 1..32: $bit" }
    return value or (1 shl (bit - 1))
  }

  /**
   * 将[value]的第[bit]位设为0
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun set0(value: Int, bit: @IntRange(from = 1, to = 32) Int): Int {
    require(bit in 1..32) { "bit must in 1..32: $bit" }
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
    require(bit in 1..32) { "bit must in 1..32: $bit" }
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
    require(bit in 1..32) { "bit must in 1..32: $bit" }
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
  fun set1(value: Long, bit: @IntRange(from = 1, to = 64) Int): Long {
    require(bit in 1..64) { "bit must in 1..64: $bit" }
    return value or (1.toLong() shl (bit - 1))
  }

  /**
   * 将[value]的第[bit]位设为0
   *
   * @param value 原值
   * @param bit 第几位，从1开始
   * @return 新值
   */
  @JvmStatic
  @CheckReturnValue
  fun set0(value: Long, bit: @IntRange(from = 1, to = 64) Int): Long {
    require(bit in 1..64) { "bit must in 1..64: $bit" }
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
    require(bit in 1..64) { "bit must in 1..64: $bit" }
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
    require(bit in 1..64) { "bit must in 1..64: $bit" }
    return (value and (1L shl (bit - 1))) == 0L
  }

  /**
   * 将[array]的第[bit]位置为1，如果[bit]>[array].size*32则自动扩容，并返回新的数组
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 如果bit<=array.size*32, 返回原数组；否则返回扩容后新的数组
   */
  @JvmStatic
  @CheckReturnValue
  fun set1(array: IntArray, bit: @Positive Int): IntArray {
    require(bit > 0) { "bit must be positive int: $bit" }
    var rem = bit and 31
    var idx = bit shr 5
    if (rem == 0) {
      rem = 31
      idx--
    }

    var result = array
    if (array.size <= idx) {
      result = array.copyOf(idx + 1)
    }
    result[idx] = set1(result[idx], rem)
    return result
  }

  /**
   * 将[array]的第[bit]位设为0
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 原[array]
   */
  @JvmStatic
  @CheckReturnValue
  fun set0(array: IntArray, bit: @Positive Int): IntArray {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 5)) {
      return array
    }
    var rem = bit and 31
    var idx = bit shr 5
    if (rem == 0) {
      rem = 31
      idx--
    }
    array[idx] = set1(array[idx], rem)
    return array
  }

  /**
   * 判断[array]的第[bit]位是否为1
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 是否为1
   */
  @JvmStatic
  fun is1(array: IntArray, bit: @Positive Int): Boolean {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 5)) {
      return false
    }
    var rem = bit and 31
    var idx = bit shr 5
    if (rem == 0) {
      rem = 31
      idx--
    }
    return is1(array[idx], rem)
  }

  /**
   * 判断[array]的第[bit]位是否为0
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 是否为0
   */
  @JvmStatic
  fun is0(array: IntArray, bit: @Positive Int): Boolean {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 5)) {
      return true
    }
    var rem = bit and 31
    var idx = bit shr 5
    if (rem == 0) {
      rem = 31
      idx--
    }
    return is0(array[idx], rem)
  }


  /**
   * 将[array]的第[bit]位置为1，如果[bit]>[array].size*64则自动扩容，并返回新的数组
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 如果bit<=array.size*64, 返回原数组；否则返回扩容后新的数组
   */
  @JvmStatic
  @CheckReturnValue
  fun set1(array: LongArray, bit: @Positive Int): LongArray {
    require(bit > 0) { "bit must be positive int: $bit" }
    var rem = bit and 63
    var idx = bit shr 6
    if (rem == 0) {
      rem = 64
      idx--
    }

    var result = array
    if (array.size <= idx) {
      result = array.copyOf(idx + 1)
    }
    result[idx] = set1(result[idx], rem)
    return result
  }

  /**
   * 将[array]的第[bit]位设为0
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 原[array]
   */
  @JvmStatic
  @CheckReturnValue
  fun set0(array: LongArray, bit: @Positive Int): LongArray {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 6)) {
      return array
    }
    var rem = bit and 63
    var idx = bit shr 6
    if (rem == 0) {
      rem = 64
      idx--
    }
    array[idx] = set1(array[idx], rem)
    return array
  }

  /**
   * 判断[array]的第[bit]位是否为1
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 是否为1
   */
  @JvmStatic
  fun is1(array: LongArray, bit: @Positive Int): Boolean {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 6)) {
      return false
    }
    var rem = bit and 63
    var idx = bit shr 6
    if (rem == 0) {
      rem = 64
      idx--
    }
    return is1(array[idx], rem)
  }

  /**
   * 判断[array]的第[bit]位是否为0
   *
   * @param array int数组
   * @param bit 第几位，从1开始
   * @return 是否为0
   */
  @JvmStatic
  fun is0(array: LongArray, bit: @Positive Int): Boolean {
    require(bit > 0) { "bit must be positive int: $bit" }
    if (bit > (array.size shl 6)) {
      return true
    }
    var rem = bit and 63
    var idx = bit shr 6
    if (rem == 0) {
      rem = 64
      idx--
    }
    return is0(array[idx], rem)
  }
}
