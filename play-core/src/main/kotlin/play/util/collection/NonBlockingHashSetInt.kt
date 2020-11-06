package play.util.collection

import com.google.common.collect.Collections2
import org.jctools.maps.NonBlockingHashMapLong
import java.util.*
import java.util.stream.IntStream
import java.util.stream.StreamSupport

class NonBlockingHashSetInt : MutableSetInt, MutableSet<Int> {
  @Transient
  private val map = NonBlockingHashMapLong<Boolean>()

  override val size: Int = map.size

  override fun add(element: Int): Boolean {
    return map.put(element.toLong(), java.lang.Boolean.TRUE) != null
  }

  override fun addAll(elements: Collection<Int>): Boolean {
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

  @Deprecated("use `iter` instead.", ReplaceWith("iter()"))
  override fun iterator(): MutableIterator<Int> {
    return iter().toJava()
  }

  @Suppress("UNCHECKED_CAST")
  override fun iter(): IntIterator {
    return object : IntIterator {
      private val it = map.keys() as NonBlockingHashMapLong<Boolean>.IteratorLong
      override fun hasNext(): Boolean = it.hasNext()

      override fun next(): Int = it.nextLong().toInt()
    }
  }

  fun stream(): IntStream {
    return StreamSupport.intStream(
      Spliterators.spliterator(iter().toJava(), size.toLong(), Spliterator.DISTINCT),
      false
    )
  }

  override fun toStream(): IntStream = stream()

  override fun remove(element: Int): Boolean {
    return map.remove(element.toLong())
  }

  override fun removeAll(elements: Collection<Int>): Boolean {
    var modified = false
    for (e in elements) {
      if (remove(e)) {
        modified = true
      }
    }
    return modified
  }

  override fun retainAll(elements: Collection<Int>): Boolean {
    return map.keys.retainAll(Collections2.transform(elements) { it?.toLong() })
  }

  override fun contains(element: Int): Boolean {
    return map.contains(element)
  }

  override fun containsAll(elements: Collection<Int>): Boolean {
    for (e in elements) {
      if (!contains(e)) {
        return false
      }
    }
    return true
  }

  override fun isEmpty(): Boolean = map.isEmpty()

  override fun toJava(): MutableSet<Int> {
    return this
  }
}
