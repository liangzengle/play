package play.example.common.collection

import org.jctools.maps.NonBlockingHashMapLong
import java.util.*
import java.util.stream.LongStream
import java.util.stream.StreamSupport

class NonBlockingHashSetLong : MutableSet<Long> {
  @Transient
  private val map = NonBlockingHashMapLong<Boolean>()

  override val size: Int = map.size

  override fun add(element: Long): Boolean {
    return map.put(element, java.lang.Boolean.TRUE) != null
  }

  override fun addAll(elements: Collection<Long>): Boolean {
    var modified = false
    for (e in elements) {
      if (add(e)) {
        modified = true
      }
    }
    return modified
  }

  override fun clear() {
    map.clear()
  }

  @Deprecated("use `iter` instead.")
  override fun iterator(): MutableIterator<Long> {
    return map.keys.iterator()
  }

  @Suppress("UNCHECKED_CAST")
  fun iter(): NonBlockingHashMapLong<Boolean>.IteratorLong {
    return map.keys() as NonBlockingHashMapLong<Boolean>.IteratorLong
  }

  fun stream(): LongStream {
    return StreamSupport.longStream(
      Spliterators.spliterator(iter().toJava(), size.toLong(), Spliterator.DISTINCT),
      false
    )
  }

  override fun remove(element: Long): Boolean {
    return map.remove(element)
  }

  override fun removeAll(elements: Collection<Long>): Boolean {
    var modified = false
    for (e in elements) {
      if (remove(e)) {
        modified = true
      }
    }
    return modified
  }

  override fun retainAll(elements: Collection<Long>): Boolean {
    return map.keys.retainAll(elements)
  }

  override fun contains(element: Long): Boolean {
    return map.contains(element)
  }

  override fun containsAll(elements: Collection<Long>): Boolean {
    for (e in elements) {
      if (!contains(e)) {
        return false
      }
    }
    return true
  }

  override fun isEmpty(): Boolean = map.isEmpty()

}

fun <T> NonBlockingHashMapLong<T>.IteratorLong.toJava(): PrimitiveIterator.OfLong {
  return object : PrimitiveIterator.OfLong {
    override fun remove() {
      return this@toJava.remove()
    }

    override fun hasNext(): Boolean {
      return this@toJava.hasNext()
    }

    override fun nextLong(): Long {
      return this@toJava.nextLong()
    }
  }
}
