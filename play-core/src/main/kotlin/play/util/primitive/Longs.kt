@file:Suppress("NOTHING_TO_INLINE", "UnstableApiUsage")

package play.util.primitive

import com.google.common.math.LongMath
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs

fun Long.isPowerOfTwo(): Boolean = LongMath.isPowerOfTwo(this)

fun Long.isEven(): Boolean = (this and 1) == 0L

fun Long.isOdd(): Boolean = !this.isEven()

infix fun Long.ceilingDivide(b: Long): Long {
  val r = this / b
  return if ((this xor b) >= 0 && (r * b != this)) r + 1 else r
}

infix fun Long.checkedAdd(b: Long): Long = LongMath.checkedAdd(this, b)

infix fun Long.checkedSubtract(b: Long): Long = LongMath.checkedSubtract(this, b)

infix fun Long.checkedMultiply(b: Long): Long = LongMath.checkedMultiply(this, b)

infix fun Long.safeAdd(b: Long): Long = LongMath.saturatedAdd(this, b)

infix fun Long.safeSubtract(b: Long): Long = LongMath.saturatedSubtract(this, b)

infix fun Long.safeMultiply(b: Long): Long = LongMath.saturatedMultiply(this, b)

fun Long.high32(): Int = (this shl 32).toInt()

fun Long.toByteArray(): ByteArray = Longs.toByteArray(this)

inline fun Long.toIntSaturated() = Ints.saturatedCast(this)

inline fun Long.toIntChecked() = Ints.checkedCast(this)

fun toLong(high: Int, low: Int): Long = high.toLong() shl 32 or (low.toLong() and 0xFFFFFFFFL)
