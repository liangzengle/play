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

val EmptyObjectArray = arrayOf<Any>()

fun IntArray.getOrDefault(index: Int, default: Int): Int {
  return if (index >= size || index < 0) default else this[index]
}
