package play.util.collection

import com.google.common.collect.ComparisonChain
import java.util.stream.IntStream

/**
 * Created by LiangZengle on 2020/2/23.
 */
interface IntTuple {
  companion object {
    @JvmStatic
    fun of(_1: Int): IntTuple1 = IntTuple1(_1)

    @JvmStatic
    fun of(_1: Int, _2: Int): IntTuple2 = IntTuple2(_1, _2)

    @JvmStatic
    fun of(_1: Int, _2: Int, _3: Int): IntTuple3 = IntTuple3(_1, _2, _3)

    @JvmStatic
    fun of(_1: Int, _2: Int, _3: Int, _4: Int): IntTuple4 = IntTuple4(_1, _2, _3, _4)

    @JvmStatic
    fun of(_1: Int, _2: Int, _3: Int, _4: Int, _5: Int): IntTuple5 = IntTuple5(_1, _2, _3, _4, _5)

    @JvmStatic
    fun of(vararg elems: Int): IntTupleN = IntTupleN(elems)
  }

  fun size(): Int

  fun toArray(): IntArray

  fun toStream(): IntStream

  val indies get() = 0 until size()

  fun get(index: Int): Int
}

inline fun IntTuple.foreach(op: (Int) -> Unit) {
  for (i in indies) {
    op(get(i))
  }
}

data class IntTuple1(val _1: Int) : IntTuple, Comparable<IntTuple1> {
  override fun size(): Int = 1

  override fun toArray(): IntArray = intArrayOf(_1)

  override fun toStream(): IntStream = IntStream.of(_1)

  override fun compareTo(other: IntTuple1): Int {
    return _1.compareTo(other._1)
  }

  override fun get(index: Int): Int {
    return if (index == 0) _1 else throw IndexOutOfBoundsException("index=$index, size=${size()}")
  }
}

data class IntTuple2(val _1: Int, val _2: Int) : IntTuple, Comparable<IntTuple2> {
  override fun compareTo(other: IntTuple2): Int {
    return ComparisonChain.start().compare(_1, other._1).compare(_2, other._2).result()
  }

  override fun size(): Int = 2

  override fun toArray(): IntArray = intArrayOf(_1, _2)

  override fun toStream(): IntStream = IntStream.of(_1, _2)

  override fun get(index: Int): Int {
    return when (index) {
      0 -> _1
      1 -> _2
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }

  fun swap(): IntTuple2 = IntTuple2(_2, _1)
}

data class IntTuple3(val _1: Int, val _2: Int, val _3: Int) : IntTuple, Comparable<IntTuple3> {
  override fun compareTo(other: IntTuple3): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .result()
  }

  override fun size(): Int = 3

  override fun toArray(): IntArray = intArrayOf(_1, _2, _3)

  override fun toStream(): IntStream = IntStream.of(_1, _2, _3)

  override fun get(index: Int): Int {
    return when (index) {
      0 -> _1
      1 -> _2
      2 -> _3
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }
}

data class IntTuple4(val _1: Int, val _2: Int, val _3: Int, val _4: Int) : IntTuple, Comparable<IntTuple4> {
  override fun compareTo(other: IntTuple4): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .result()
  }

  override fun size(): Int = 4

  override fun toArray(): IntArray = intArrayOf(_1, _2, _3, _4)

  override fun toStream(): IntStream = IntStream.of(_1, _2, _3, _4)

  override fun get(index: Int): Int {
    return when (index) {
      0 -> _1
      1 -> _2
      2 -> _3
      3 -> _4
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }
}

data class IntTuple5(val _1: Int, val _2: Int, val _3: Int, val _4: Int, val _5: Int) : IntTuple,
  Comparable<IntTuple5> {
  override fun compareTo(other: IntTuple5): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .compare(_5, other._5)
      .result()
  }

  override fun size(): Int = 5

  override fun toArray(): IntArray = intArrayOf(_1, _2, _3, _4, _5)

  override fun toStream(): IntStream = IntStream.of(_1, _2, _3, _4, _5)

  override fun get(index: Int): Int {
    return when (index) {
      0 -> _1
      1 -> _2
      2 -> _3
      3 -> _4
      4 -> _5
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }
}

class IntTupleN constructor(private val elems: IntArray) : IntTuple, Comparable<IntTupleN> {
  @Transient
  private var hashCode = 0

  override fun size(): Int = elems.size

  override fun toArray(): IntArray = elems.copyOf()

  override fun toStream(): IntStream = elems.stream()

  override fun get(index: Int): Int {
    if (index < 0 || index >= elems.size) {
      throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
    return elems[index]
  }

  override fun compareTo(other: IntTupleN): Int {
    if (size() > other.size()) return 1
    if (size() < other.size()) return -1
    for (i in 0..size()) {
      val r = this.get(i).compareTo(other.get(i))
      if (r != 0) return r
    }
    return 0
  }

  override fun equals(other: Any?): Boolean {
    if (other !is IntTupleN) return false
    return elems.contentEquals(other.elems)
  }

  override fun hashCode(): Int {
    if (hashCode != 0) return hashCode
    hashCode = elems.contentHashCode()
    return hashCode
  }

  override fun toString(): String {
    return elems.contentToString()
  }
}
