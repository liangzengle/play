package play.util.collection

import com.google.common.primitives.Longs
import java.util.stream.LongStream

class ImmutableLongArrayList private constructor(val elems: LongArray) : LongArrayList, RandomAccess {
  override val size: Int
    get() = elems.size

  override fun get(index: Int): Long {
    return elems[index]
  }

  override fun asStream(): LongStream {
    return elems.stream()
  }

  override fun iterator(): LongIterator {
    return object : LongIterator {
      private var i = 0
      override fun hasNext(): Boolean {
        return i in 0 until size
      }

      override fun next(): Long {
        val elem = elems[i]
        i += 1
        return elem
      }
    }
  }

  fun contains(element: Long): Boolean {
    return elems.contains(element)
  }

  fun indexOf(element: Long): Int {
    return elems.indexOf(element)
  }

  override fun isEmpty(): Boolean {
    return elems.isEmpty()
  }

  fun lastIndexOf(element: Long): Int {
    return elems.lastIndexOf(element)
  }

  override fun toJava(): List<Long> {
    return Longs.asList(*elems)
  }

  companion object {
    @JvmStatic
    private val Empty = ImmutableLongArrayList(EmptyLongArray)

    @JvmStatic
    fun empty() = Empty

    @JvmStatic
    fun copyOf(elems: LongArray): ImmutableLongArrayList {
      return of(*elems.copyOf())
    }

    @JvmStatic
    fun copyOf(elems: LongArray, fromIndex: Int, toIndex: Int): ImmutableLongArrayList {
      return of(*elems.copyOfRange(fromIndex, toIndex))
    }

    @JvmStatic
    fun wrapOf(elems: LongArray): ImmutableLongArrayList {
      return if (elems.isEmpty()) empty() else ImmutableLongArrayList(elems)
    }

    @JvmStatic
    fun of(vararg elems: Long): ImmutableLongArrayList {
      return if (elems.isEmpty()) empty() else ImmutableLongArrayList(elems)
    }
  }
}
