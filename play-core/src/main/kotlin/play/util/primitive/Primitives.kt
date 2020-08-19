package play.util.primitive

import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import com.google.common.primitives.Shorts
import kotlin.math.ceil

/**
 * Created by LiangZengle on 2020/2/20.
 */
fun toLong(high: Int, low: Int): Long = high.toLong() shl 32 or (low.toLong() and 0xFFFFFFFFL)

fun toInt(high: Short, low: Short): Int = high.toInt() shl 16 or (low.toInt() and 0xFFFF)

fun Boolean.toByte(): Byte = if (this) 1 else 0

fun Int.toBoolean(): Boolean = this != 0

fun Byte.toBoolean(): Boolean = this != 0.toByte()

fun readAsShort(bytes: ByteArray): Short = Shorts.fromByteArray(bytes)

fun readAsInt(bytes: ByteArray): Int = Ints.fromByteArray(bytes)

fun readAsLong(bytes: ByteArray): Long = Longs.fromByteArray(bytes)

fun Float.ceilToInt(): Int = ceil(this).toInt()

fun Double.ceilToInt(): Int = ceil(this).toInt()

fun Double.ceilToLong(): Long = ceil(this).toLong()
