package play.util.collection

import java.util.stream.IntStream

/**
 *
 * @author LiangZengle
 */
interface MutableSetInt {

  fun add(element: Int): Boolean

  fun addAll(elements: Collection<Int>): Boolean

  fun clear()

  fun iter(): IntIterator

  fun remove(element: Int): Boolean

  val size: Int

  fun contains(element: Int): Boolean

  fun isEmpty(): Boolean

  fun toStream(): IntStream

  fun toJava(): MutableSet<Int>
}
