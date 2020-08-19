package play.util.collection

import java.util.function.IntConsumer

interface IntIterable {
  operator fun iterator(): IntIterator
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntIterable.forEach(f: IntConsumer) {
  for (elem in this) {
    f.accept(elem)
  }
}
