package play.util.collection

import java.util.function.IntConsumer

interface IntIterable {
  operator fun iterator(): IntIterator

  fun toJava(): Iterable<Int> {
    @Suppress("UNCHECKED_CAST")
    if (Iterable::class.java.isAssignableFrom(javaClass)) return this as Iterable<Int>
    return object : Iterable<Int> {
      override fun iterator(): Iterator<Int> {
        return this@IntIterable.iterator().toJava()
      }
    }
  }

  companion object {
    @JvmStatic
    fun fromJava(it: Iterable<Int>): IntIterable {
      return when (it) {
        is IntIterable -> it
        else -> object : IntIterable {
          override fun iterator(): IntIterator {
            return IntIterator.fromJava(it.iterator())
          }
        }
      }
    }
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntIterable.forEach(f: IntConsumer) {
  for (elem in this) {
    f.accept(elem)
  }
}
