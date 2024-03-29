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

infix fun Int.checkedMultiply(b: Int): Int = IntMath.checkedMultiply(this, b)

infix fun Int.safeAdd(b: Int): Int = IntMath.saturatedAdd(this, b)

infix fun Int.safeSubtract(b: Int): Int = IntMath.saturatedSubtract(this, b)

infix fun Int.safeMultiply(b: Int): Int = IntMath.saturatedMultiply(this, b)

fun Int.toByteArray(): ByteArray = Ints.toByteArray(this)

fun Int.high16(): Short = (this shr 16).toShort()

fun Int.low16(): Short = this.toShort()

fun Int.toByteChecked(): Byte {
  if (this < Byte.MIN_VALUE || this > Byte.MAX_VALUE) {
    throw ArithmeticException("overflow: $this")
  }
  return this.toByte()
}

fun Int.toByteSaturated(): Byte {
  return when {
    this < Byte.MIN_VALUE -> {
      Byte.MIN_VALUE
    }

    this > Byte.MAX_VALUE -> {
      Byte.MAX_VALUE
    }

    else -> this.toByte()
  }
}

fun Int.toShortChecked(): Short {
  if (this < Short.MIN_VALUE || this > Short.MAX_VALUE) {
    throw ArithmeticException("overflow: $this")
  }
  return this.toShort()
}

fun Int.toShortSaturated(): Short {
  return when {
    this < Short.MIN_VALUE -> Short.MIN_VALUE
    this > Short.MAX_VALUE -> Short.MAX_VALUE
    else -> this.toShort()
  }
}

fun toInt(high: Short, low: Short): Int = high.toInt() shl 16 or (low.toInt() and 0xFFFF)
