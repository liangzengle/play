package play.util.collection

import java.util.stream.IntStream

/**
 *
 * @author LiangZengle
 */
interface MutableSetInt : IntIterable {

  fun add(element: Int): Boolean

  fun addAll(elements: Collection<Int>): Boolean

  fun addAll(elements: MutableSetInt): Boolean

  fun clear()

  fun remove(element: Int): Boolean

  fun removeAll(elements: Collection<Int>): Boolean

  fun removeAll(elements: MutableSetInt): Boolean

  fun retainAll(elements: Collection<Int>): Boolean

  fun retainAll(elements: MutableSetInt): Boolean

  val size: Int

  fun contains(element: Int): Boolean

  fun containsAll(elements: Collection<Int>): Boolean

  fun containsAll(elements: MutableSetInt): Boolean

  fun isEmpty(): Boolean

  fun stream(): IntStream

  override fun toJava(): MutableSet<Int>
}
