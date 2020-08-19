@file:JvmName("ArrayUtil")

package play.util

import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import com.google.common.primitives.Shorts

val EmptyBooleanArray = booleanArrayOf()
val EmptyByteArray = byteArrayOf()
val EmptyCharArray = charArrayOf()
val EmptyShortArray = shortArrayOf()
val EmptyIntArray = intArrayOf()
val EmptyLongArray = longArrayOf()
val EmptyFloatArray = floatArrayOf()
val EmptyDoubleArray = doubleArrayOf()

val EmptyStringArray = arrayOf<String>()

val EmptyClassArray = arrayOf<Class<*>>()

val EmptyObjectArray = arrayOf<Any>()

fun ByteArray.getOrDefault(index: Int, default: Byte): Byte {
  return if (index >= size || index < 0) default else this[index]
}

fun ShortArray.getOrDefault(index: Int, default: Short): Short {
  return if (index >= size || index < 0) default else this[index]
}

fun IntArray.getOrDefault(index: Int, default: Int): Int {
  return if (index >= size || index < 0) default else this[index]
}

fun LongArray.getOrDefault(index: Int, default: Long): Long {
  return if (index >= size || index < 0) default else this[index]
}

fun DoubleArray.getOrDefault(index: Int, default: Double): Double {
  return if (index >= size || index < 0) default else this[index]
}

fun ByteArray.readShort(): Short = Shorts.fromByteArray(this)

fun ByteArray.readShort(fromIndex: Int): Short = Shorts.fromBytes(this[fromIndex], this[fromIndex + 1])

fun ByteArray.readInt(): Int = Ints.fromByteArray(this)

fun ByteArray.readInt(fromIndex: Int): Int =
  Ints.fromBytes(this[fromIndex], this[fromIndex + 1], this[fromIndex + 2], this[fromIndex + 3])

fun ByteArray.readLong(): Long = Longs.fromByteArray(this)

fun ByteArray.readLong(fromIndex: Int): Long =
  Longs.fromBytes(
    this[fromIndex],
    this[fromIndex + 1],
    this[fromIndex + 2],
    this[fromIndex + 3],
    this[fromIndex + 4],
    this[fromIndex + 5],
    this[fromIndex + 6],
    this[fromIndex + 7]
  )
