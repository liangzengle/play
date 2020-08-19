package play.util.collection

import com.google.common.collect.ComparisonChain
import java.util.stream.LongStream

/**
 * Created by LiangZengle on 2020/2/23.
 */
interface LongTuple {
  companion object {
    @JvmStatic
    fun of(_1: Long, _2: Long): LongTuple2 = LongTuple2(_1, _2)

    @JvmStatic
    fun of(_1: Long, _2: Long, _3: Long): LongTuple3 = LongTuple3(_1, _2, _3)

    @JvmStatic
    fun of(_1: Long, _2: Long, _3: Long, _4: Long): LongTuple4 = LongTuple4(_1, _2, _3, _4)

    @JvmStatic
    fun of(_1: Long, _2: Long, _3: Long, _4: Long, _5: Long): LongTuple5 = LongTuple5(_1, _2, _3, _4, _5)
  }

  fun size(): Long

  fun toArray(): LongArray

  fun toStream(): LongStream
}

data class LongTuple2(val _1: Long, val _2: Long) : LongTuple, Comparable<LongTuple2> {
  override fun compareTo(other: LongTuple2): Int {
    return ComparisonChain.start().compare(_1, other._1).compare(_2, other._2).result()
  }

  override fun size(): Long = 2

  override fun toArray(): LongArray = longArrayOf(_1, _2)

  override fun toStream(): LongStream = LongStream.of(_1, _2)
}

data class LongTuple3(val _1: Long, val _2: Long, val _3: Long) : LongTuple, Comparable<LongTuple3> {
  override fun compareTo(other: LongTuple3): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .result()
  }

  override fun size(): Long = 3

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3)
}

data class LongTuple4(val _1: Long, val _2: Long, val _3: Long, val _4: Long) : LongTuple, Comparable<LongTuple4> {
  override fun compareTo(other: LongTuple4): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .result()
  }

  override fun size(): Long = 4

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3, _4)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3, _4)
}

data class LongTuple5(val _1: Long, val _2: Long, val _3: Long, val _4: Long, val _5: Long) : LongTuple,
  Comparable<LongTuple5> {
  override fun compareTo(other: LongTuple5): Int {
    return ComparisonChain.start()
      .compare(_1, other._1)
      .compare(_2, other._2)
      .compare(_3, other._3)
      .compare(_4, other._4)
      .compare(_5, other._5)
      .result()
  }

  override fun size(): Long = 5

  override fun toArray(): LongArray = longArrayOf(_1, _2, _3, _4, _5)

  override fun toStream(): LongStream = LongStream.of(_1, _2, _3, _4, _5)
}
