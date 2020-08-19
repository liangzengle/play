package play.util.collection

import com.google.common.primitives.Ints
import java.util.*
import java.util.stream.IntStream

class ImmutableIntArrayList internal constructor(private val elems: IntArray) : IntArrayList, RandomAccess {

  override fun get(index: Int): Int {
    return elems[index]
  }

  override val size: Int
    get() = elems.size

  fun contains(element: Int): Boolean {
    for (elem in elems) {
      if (elem == element) {
        return true
      }
    }
    return false
  }

  fun indexOf(element: Int): Int {
    return elems.indexOf(element)
  }

  override fun isEmpty(): Boolean {
    return elems.isEmpty()
  }

  override fun iterator(): IntIterator {
    return object : IntIterator {
      private var i = 0
      override fun hasNext(): Boolean {
        return size > i
      }

      override fun next(): Int {
        val elem = elems[i]
        i += 1
        return elem
      }
    }
  }

  fun subList(fromIndex: Int, toIndex: Int): ImmutableIntArrayList {
    if (fromIndex > toIndex) throw IllegalArgumentException("fromIndex > toIndex")
    if (fromIndex < 0 || fromIndex >= size) {
      throw ArrayIndexOutOfBoundsException("fromIndex=$fromIndex, size=$size")
    }
    if (toIndex < 0 || toIndex >= size) {
      throw ArrayIndexOutOfBoundsException("toIndex=$toIndex, size=$size")
    }
    return ImmutableIntArrayList(elems.copyOfRange(fromIndex, toIndex))
  }

  override fun asStream(): IntStream {
    return Arrays.stream(elems)
  }

  override fun toJava(): List<Int> {
    return Ints.asList(*elems)
  }

  companion object {
    private val Empty = ImmutableIntArrayList(EmptyIntArray)

    @JvmStatic
    fun empty() = Empty

    @JvmStatic
    fun copyOf(elems: IntArray): ImmutableIntArrayList {
      return of(*elems.copyOf())
    }

    @JvmStatic
    fun copyOf(elems: IntArray, fromIndex: Int, toIndex: Int): ImmutableIntArrayList {
      return of(*elems.copyOfRange(fromIndex, toIndex))
    }

    @JvmStatic
    fun wrapOf(elems: IntArray): ImmutableIntArrayList {
      return if (elems.isEmpty()) empty() else ImmutableIntArrayList(elems)
    }

    @JvmStatic
    fun of(vararg elems: Int): ImmutableIntArrayList {
      return if (elems.isEmpty()) empty() else ImmutableIntArrayList(elems)
    }
  }
}
