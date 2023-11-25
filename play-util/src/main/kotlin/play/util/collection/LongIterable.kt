package play.util.collection

import java.util.function.LongConsumer

interface LongIterable {
  operator fun iterator(): LongIterator

  fun toJava(): Iterable<Long> {
    @Suppress("UNCHECKED_CAST")
    if (Iterable::class.java.isAssignableFrom(javaClass)) return this as Iterable<Long>
    return object : Iterable<Long> {
      override fun iterator(): Iterator<Long> {
        return this@LongIterable.iterator().toJava()
      }
    }
  }

  companion object {
    @JvmStatic
    fun fromJava(it: Iterable<Long>): LongIterable {
      return when (it) {
        is LongIterable -> it
        else -> object : LongIterable {
          override fun iterator(): LongIterator {
            return LongIterator.fromJava(it.iterator())
          }
        }
      }
    }
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun LongIterable.forEach(f: LongConsumer) {
  for (elem in this) {
    f.accept(elem)
  }
}
