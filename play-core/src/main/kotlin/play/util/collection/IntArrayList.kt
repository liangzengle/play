package play.util.collection

import java.util.function.IntConsumer
import java.util.stream.IntStream

interface IntArrayList : RandomAccess {

  val size: Int

  fun get(index: Int): Int

  fun isEmpty(): Boolean = size == 0

  fun isNotEmpty(): Boolean = size != 0

  fun asStream(): IntStream

  fun iterator(): IntIterator

  fun foreach(consumer: IntConsumer) {
    for (i in 0 until size) {
      consumer.accept(get(i))
    }
  }

  fun toJava(): List<Int>
}

@Suppress("NOTHING_TO_INLINE")
inline fun intListOf(vararg elems: Int): IntArrayList = ImmutableIntArrayList.wrapOf(elems)

@Suppress("NOTHING_TO_INLINE", "ReplaceRangeToWithUntil")
inline fun IntArrayList.indices() = 0..size - 1
