package play.util.collection

import java.util.*

interface LongIterator {
  operator fun hasNext(): Boolean
  operator fun next(): Long

  companion object {
    @JvmStatic
    fun fromJava(it: Iterator<Long>): LongIterator {
      return when (it) {
        is LongIterator -> it
        is PrimitiveIterator.OfLong -> object : LongIterator {
          override fun hasNext(): Boolean {
            return it.hasNext()
          }

          override fun next(): Long {
            return it.nextLong()
          }
        }

        else -> object : LongIterator {
          override fun hasNext(): Boolean {
            return it.hasNext()
          }

          override fun next(): Long {
            return it.next()
          }
        }
      }
    }
  }

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
