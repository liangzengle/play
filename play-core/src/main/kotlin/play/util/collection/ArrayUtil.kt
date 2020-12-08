@file:JvmName("ArrayUtil")

package play.util.collection

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

fun IntArray.getOrDefault(index: Int, default: Int): Int {
  return if (index >= size || index < 0) default else this[index]
}

fun ByteArray.getOrDefault(index: Int, default: Byte): Byte {
  return if (index >= size || index < 0) default else this[index]
}

fun ShortArray.getOrDefault(index: Int, default: Short): Short {
  return if (index >= size || index < 0) default else this[index]
}

fun LongArray.getOrDefault(index: Int, default: Long): Long {
  return if (index >= size || index < 0) default else this[index]
}

fun DoubleArray.getOrDefault(index: Int, default: Double): Double {
  return if (index >= size || index < 0) default else this[index]
}
