package play.util.collection

import java.util.*

interface IntIterator {
  operator fun hasNext(): Boolean
  operator fun next(): Int

  fun toJava(): PrimitiveIterator.OfInt {
    return object : PrimitiveIterator.OfInt {
      override fun remove() {
        throw UnsupportedOperationException()
      }

      override fun hasNext(): Boolean {
        return this@IntIterator.hasNext()
      }

      override fun nextInt(): Int {
        return this@IntIterator.next()
      }
    }
  }
}
