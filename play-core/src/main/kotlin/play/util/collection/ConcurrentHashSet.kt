package play.util.collection

import java.util.concurrent.ConcurrentHashMap

/**
 *
 * @author LiangZengle
 */
class ConcurrentHashSet<E> : MutableSet<E> {

  @Transient
  private val set = ConcurrentHashMap<E, Boolean>().keySet(java.lang.Boolean.TRUE)


  override fun add(element: E): Boolean = set.add(element)

  override fun addAll(elements: Collection<E>): Boolean = set.addAll(elements)

  override fun clear() = set.clear()

  override fun iterator(): MutableIterator<E> = set.iterator()

  override fun remove(element: E): Boolean = set.remove(element)

  override fun removeAll(elements: Collection<E>): Boolean = set.removeAll(elements)

  override fun retainAll(elements: Collection<E>): Boolean = set.retainAll(elements)

  override val size: Int
    get() = set.size

  override fun contains(element: E): Boolean = set.contains(element)

  override fun containsAll(elements: Collection<E>): Boolean = set.containsAll(elements)

  override fun isEmpty(): Boolean = set.isEmpty()
}
