@file:Suppress("UnstableApiUsage", "NOTHING_TO_INLINE")

package play.util.primitive

import com.google.common.math.IntMath
import com.google.common.primitives.Ints

inline fun Int.ceilingPowerOfTwo(): Int = IntMath.ceilingPowerOfTwo(this)

inline fun Int.isPowerOfTwo(): Boolean = IntMath.isPowerOfTwo(this)

fun Int.isEven(): Boolean = (this and 1) == 0

fun Int.isOdd(): Boolean = !this.isEven()

infix fun Int.ceilingDivide(b: Int): Int {
  val r = this / b
  return if ((this xor b) >= 0 && (r * b != this)) r + 1 else r
}

infix fun Int.checkedAdd(b: Int): Int = IntMath.checkedAdd(this, b)

infix fun Int.checkedSubtract(b: Int): Int = IntMath.checkedSubtract(this, b)

infix fun Int.checkedMultiply(b: Int): Int = IntMath.checkedSubtract(this, b)

infix fun Int.safeAdd(b: Int): Int = IntMath.saturatedAdd(this, b)

infix fun Int.safeSubtract(b: Int): Int = IntMath.saturatedSubtract(this, b)

infix fun Int.safeMultiply(b: Int): Int = IntMath.saturatedSubtract(this, b)

/**
 * 将第n位置为1
 * @param bit 第几位(从1开始)
 */
infix fun Int.setBit(bit: Int): Int = this or (1 shl (bit - 1))

/**
 * 将第n位置为0
 * @param bit 第几位(从1开始)
 */
infix fun Int.clearBit(bit: Int): Int = this and (1 shl (bit - 1)).inv()

fun Int.toByteArray(): ByteArray = Ints.toByteArray(this)

fun Int.high16(): Short = (this shr 16).toShort()

fun Int.low16(): Short = this.toShort()
