@file:JvmName("Lang")

package play.util

import java.util.*

fun String.splitToInts(delimiter: Char, radix: Int = 10): PrimitiveIterator.OfInt {
  return object : PrimitiveIterator.OfInt {
    var start = 0;
    var end = indexOf(delimiter)

    override fun remove() {
      throw UnsupportedOperationException()
    }

    override fun hasNext(): Boolean {
      return end != -1
    }

    override fun nextInt(): Int {
      var value = 0
      for (i in start ..< end) {
        value = value * radix + (get(i).digitToInt(radix))
      }
      start = end + 1
      end = indexOf(delimiter, start)
      if (end == -1 && start < this@splitToInts.length) {
        end = this@splitToInts.length
      }
      return value
    }
  }
}
