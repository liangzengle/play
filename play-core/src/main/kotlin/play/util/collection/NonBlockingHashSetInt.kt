package play.util.collection

import com.google.common.collect.Collections2
import org.jctools.maps.NonBlockingHashMapLong
import java.util.*
import java.util.stream.IntStream
import java.util.stream.StreamSupport

class NonBlockingHashSetInt : MutableSetInt {
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

  override fun addAll(elements: MutableSetInt): Boolean {
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

  override fun iterator(): IntIterator {
    return object : IntIterator {
      @Suppress("UNCHECKED_CAST")
      private val it = map.keys() as NonBlockingHashMapLong<Boolean>.IteratorLong
      override fun hasNext(): Boolean = it.hasNext()

      override fun next(): Int = it.nextLong().toInt()
    }
  }

  override fun stream(): IntStream {
    return StreamSupport.intStream(
      Spliterators.spliterator(iterator().toJava(), size.toLong(), Spliterator.DISTINCT),
      false
    )
  }

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

  override fun removeAll(elements: MutableSetInt): Boolean {
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

  override fun retainAll(elements: MutableSetInt): Boolean {
    return map.keys.retainAll(Collections2.transform(elements.toJava()) { it?.toLong() })
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

  override fun containsAll(elements: MutableSetInt): Boolean {
    for (e in elements) {
      if (!contains(e)) {
        return false
      }
    }
    return true
  }

  override fun isEmpty(): Boolean = map.isEmpty()

  override fun toJava(): MutableSet<Int> {
    return object : MutableSet<Int> {
      override fun add(element: Int): Boolean {
        return this@NonBlockingHashSetInt.add(element)
      }

      override fun addAll(elements: Collection<Int>): Boolean {
        return this@NonBlockingHashSetInt.addAll(elements)
      }

      override fun clear() {
        return this@NonBlockingHashSetInt.clear()
      }

      override fun iterator(): MutableIterator<Int> {
        return this@NonBlockingHashSetInt.iterator().toJava()
      }

      override fun remove(element: Int): Boolean {
        return this@NonBlockingHashSetInt.remove(element)
      }

      override fun removeAll(elements: Collection<Int>): Boolean {
        return this@NonBlockingHashSetInt.removeAll(elements)
      }

      override fun retainAll(elements: Collection<Int>): Boolean {
        return this@NonBlockingHashSetInt.retainAll(elements)
      }

      override val size: Int = this@NonBlockingHashSetInt.size

      override fun contains(element: Int): Boolean {
        return this@NonBlockingHashSetInt.contains(element)
      }

      override fun containsAll(elements: Collection<Int>): Boolean {
        return this@NonBlockingHashSetInt.containsAll(elements)
      }

      override fun isEmpty(): Boolean {
        return this@NonBlockingHashSetInt.isEmpty()
      }
    }
  }
}
