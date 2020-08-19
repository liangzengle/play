package play.util.collection

import java.util.*

interface LongIterator {
  operator fun hasNext(): Boolean
  operator fun next(): Long

  @JvmDefault
  fun toJava(): PrimitiveIterator.OfLong {
    return object : PrimitiveIterator.OfLong {
      override fun remove() {
        throw UnsupportedOperationException()
      }

      override fun hasNext(): Boolean {
        return this@LongIterator.hasNext()
      }

      override fun nextLong(): Long {
        return this@LongIterator.next()
      }
    }
  }
}
