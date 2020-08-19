package play.util.collection

import java.util.function.LongConsumer

interface LongIterable {
  operator fun iterator(): LongIterator
}

@Suppress("NOTHING_TO_INLINE")
inline fun LongIterable.forEach(f: LongConsumer) {
  for (elem in this) {
    f.accept(elem)
  }
}
