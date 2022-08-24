package play.util.collection

import java.util.stream.LongStream

/**
 *
 * @author LiangZengle
 */
interface MutableSetLong : LongIterable {

  fun add(element: Long): Boolean

  fun addAll(elements: Collection<Long>): Boolean

  fun addAll(elements: MutableSetLong): Boolean

  fun clear()

  fun remove(element: Long): Boolean

  fun removeAll(elements: Collection<Long>): Boolean

  fun removeAll(elements: MutableSetLong): Boolean

  fun retainAll(elements: Collection<Long>): Boolean

  fun retainAll(elements: MutableSetLong): Boolean

  val size: Int

  fun contains(element: Long): Boolean

  fun containsAll(elements: Collection<Long>): Boolean

  fun containsAll(elements: MutableSetLong): Boolean

  fun isEmpty(): Boolean

  fun stream(): LongStream

  override fun toJava(): MutableSet<Long>
}
