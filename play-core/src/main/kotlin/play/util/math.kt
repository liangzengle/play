@file:Suppress("NOTHING_TO_INLINE")

package play.util

inline infix fun Byte.max(b: Byte): Byte = coerceAtLeast(b)

inline infix fun Short.max(b: Short): Short = coerceAtLeast(b)

inline infix fun Int.max(b: Int): Int = coerceAtLeast(b)

inline infix fun Long.max(b: Long): Long = coerceAtLeast(b)

inline infix fun Float.max(b: Float): Float = coerceAtLeast(b)

inline infix fun Double.max(b: Double): Double = coerceAtLeast(b)

inline infix fun Byte.min(b: Byte): Byte = coerceAtMost(b)

inline infix fun Short.min(b: Short): Short = coerceAtMost(b)

inline infix fun Int.min(b: Int): Int = coerceAtMost(b)

inline infix fun Long.min(b: Long): Long = coerceAtMost(b)

inline infix fun Float.min(b: Float): Float = coerceAtMost(b)

inline infix fun Double.min(b: Double): Double = coerceAtMost(b)
