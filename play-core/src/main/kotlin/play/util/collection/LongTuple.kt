package play.util.collection

import com.google.common.collect.ComparisonChain
import java.util.stream.LongStream

/**
 * Created by LiangZengle on 2020/2/23.
 */
abstract class LongTuple {
  companion object {
    @JvmName("of")
    @JvmStatic
    operator fun invoke(_1: Long): LongTuple1 = LongTuple1(_1)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(_1: Long, _2: Long): LongTuple2 = LongTuple2(_1, _2)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(_1: Long, _2: Long, _3: Long): LongTuple3 = LongTuple3(_1, _2, _3)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(_1: Long, _2: Long, _3: Long, _4: Long): LongTuple4 = LongTuple4(_1, _2, _3, _4)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(_1: Long, _2: Long, _3: Long, _4: Long, _5: Long): LongTuple5 = LongTuple5(_1, _2, _3, _4, _5)

    @JvmStatic
    operator fun invoke(vararg elems: Long): LongTuple {
      return when (elems.size) {
        1 -> LongTuple(elems[0])
        2 -> LongTuple(elems[0], elems[1])
        3 -> LongTuple(elems[0], elems[1], elems[2])
        4 -> LongTuple(elems[0], elems[1], elems[2], elems[3])
        5 -> LongTuple(elems[0], elems[1], elems[2], elems[3], elems[4])
        else -> LongTupleN(elems)
      }
    }

    private fun toString(tuple: LongTuple): String {
      val b = StringBuilder(tuple.size() * 2 + 2)
      b.append('(')
      for (i in tuple.indies) {
        if (i != 0) {
          b.append(',')
        }
        b.append(i)
      }
      b.append(')')
      return b.toString()
    }
  }

  abstract fun size(): Int

  abstract fun toArray(): LongArray

  abstract fun toStream(): LongStream

  val indies get() = 0 ..< size()

  abstract fun get(index: Int): Long

  override fun toString(): String {
    return Companion.toString(this)
  }
}

inline fun LongTuple.foreach(op: (Long) -> Unit) {
  for (i in indies) {
    op(get(i))
  }
}

data class LongTuple1(val _1: Long) : LongTuple(), Comparable<LongTuple1> {
  override fun size(): Int = 1

  override fun toArray(): LongArray = longArrayOf(_1)

  override fun toStream(): LongStream = LongStream.of(_1)

  override fun compareTo(other: LongTuple1): Int {
    return _1.compareTo(other._1)
  }

  override fun get(index: Int): Long {
    return if (index == 0) _1 else throw IndexOutOfBoundsException("index=$index, size=${size()}")
  }
}

data class LongTuple2(val _1: Long, val _2: Long) : LongTuple(), Comparable<LongTuple2> {
  override fun compareTo(other: LongTuple2): Int {
    return ComparisonChain.start().compare(_1, other._1).compare(_2, other._2).result()
  }

  override fun size(): Int = 2

  override fun toArray(): LongArray = longArrayOf(_1, _2)

  override fun toStream(): LongStream = LongStream.of(_1, _2)

  override fun get(index: Int): Long {
    return when (index) {
      0 -> _1
      1 -> _2
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }

  fun swap(): LongTuple2 = LongTuple2(_2, _1)
}

data class LongTuple3(val _1: Long, val _2: Long, val _3: Long) : LongTuple(), Comparable<LongTuple3> {
  override fun compareTo(other: LongTuple3): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .result()
  }

  override fun size(): Int = 3

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3)

  override fun get(index: Int): Long {
    return when (index) {
      0 -> _1
      1 -> _2
      2 -> _3
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }
}

data class LongTuple4(val _1: Long, val _2: Long, val _3: Long, val _4: Long) : LongTuple(), Comparable<LongTuple4> {
  override fun compareTo(other: LongTuple4): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .result()
  }

  override fun size(): Int = 4

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3, _4)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3, _4)

  override fun get(index: Int): Long {
    return when (index) {
      0 -> _1
      1 -> _2
      2 -> _3
      3 -> _4
      else -> throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
  }
}

data class LongTuple5(val _1: Long, val _2: Long, val _3: Long, val _4: Long, val _5: Long) :
  LongTuple(), Comparable<LongTuple5> {
  override fun compareTo(other: LongTuple5): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .compare(_5, other._5)
      .result()
  }

  override fun size(): Int = 5

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3, _4, _5)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3, _4, _5)

  override fun get(index: Int): Long {
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

class LongTupleN constructor(private val elems: LongArray) : LongTuple(), Comparable<LongTupleN> {
  @Transient
  private var hashCode = 0

  override fun size(): Int = elems.size

  override fun toArray(): LongArray = elems.copyOf()

  override fun toStream(): LongStream = elems.stream()

  override fun get(index: Int): Long {
    if (index < 0 || index >= elems.size) {
      throw IndexOutOfBoundsException("index=$index, size=${size()}")
    }
    return elems[index]
  }

  override fun compareTo(other: LongTupleN): Int {
    if (size() > other.size()) return 1
    if (size() < other.size()) return -1
    for (i in 0..size()) {
      val r = this.get(i).compareTo(other.get(i))
      if (r != 0) return r
    }
    return 0
  }

  override fun equals(other: Any?): Boolean {
    if (other !is LongTupleN) return false
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
