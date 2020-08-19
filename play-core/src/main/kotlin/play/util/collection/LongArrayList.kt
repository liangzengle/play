package play.util.collection

import java.util.function.LongConsumer
import java.util.stream.LongStream

interface LongArrayList : RandomAccess {
  val size: Int
  fun get(index: Int): Long
  fun isEmpty(): Boolean = size == 0
  fun isNotEmpty(): Boolean = size != 0
  fun asStream(): LongStream
  fun iterator(): LongIterator
  fun foreach(consumer: LongConsumer) {
    for (i in 0 until size) {
      consumer.accept(get(i))
    }
  }

  fun toJava(): List<Long>
}

@Suppress("NOTHING_TO_INLINE")
inline fun longListOf(vararg elems: Long): LongArrayList = ImmutableLongArrayList.wrapOf(elems)

@Suppress("NOTHING_TO_INLINE", "ReplaceRangeToWithUntil")
inline fun LongArrayList.indices() = 0..size - 1
