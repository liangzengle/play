package play.util.collection

import java.util.function.LongConsumer

interface LongIterable {
  operator fun iterator(): LongIterator
}

inline fun LongIterable.forEach(f: LongConsumer) {
  for (elem in this) {
    f.accept(elem)
  }
}
