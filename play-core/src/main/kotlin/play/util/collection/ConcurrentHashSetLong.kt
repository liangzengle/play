package play.util.collection

import org.jctools.maps.NonBlockingHashMapLong
import java.util.*
import java.util.stream.LongStream
import java.util.stream.StreamSupport

class ConcurrentHashSetLong : MutableSetLong {
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

  override fun addAll(elements: MutableSetLong): Boolean {
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

  override fun iterator(): LongIterator {
    return object : LongIterator {
      @Suppress("UNCHECKED_CAST")
      private val it = map.keys() as NonBlockingHashMapLong<Boolean>.IteratorLong
      override fun hasNext(): Boolean = it.hasNext()

      override fun next(): Long = it.nextLong()
    }
  }

  override fun stream(): LongStream {
    return StreamSupport.longStream(
      Spliterators.spliterator(iterator().toJava(), size.toLong(), Spliterator.DISTINCT),
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

  override fun removeAll(elements: MutableSetLong): Boolean {
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

  override fun retainAll(elements: MutableSetLong): Boolean {
    return map.keys.retainAll(elements.toJava())
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

  override fun containsAll(elements: MutableSetLong): Boolean {
    for (e in elements) {
      if (!contains(e)) {
        return false
      }
    }
    return true
  }

  override fun isEmpty(): Boolean = map.isEmpty()

  override fun toJava(): MutableSet<Long> {
    return object : MutableSet<Long> {
      override fun add(element: Long): Boolean {
        return this@ConcurrentHashSetLong.add(element)
      }

      override fun addAll(elements: Collection<Long>): Boolean {
        return this@ConcurrentHashSetLong.addAll(elements)
      }

      override fun clear() {
        return this@ConcurrentHashSetLong.clear()
      }

      override fun iterator(): MutableIterator<Long> {
        return this@ConcurrentHashSetLong.iterator().toJava()
      }

      override fun remove(element: Long): Boolean {
        return this@ConcurrentHashSetLong.remove(element)
      }

      override fun removeAll(elements: Collection<Long>): Boolean {
        return this@ConcurrentHashSetLong.removeAll(elements)
      }

      override fun retainAll(elements: Collection<Long>): Boolean {
        return this@ConcurrentHashSetLong.retainAll(elements)
      }

      override val size: Int = this@ConcurrentHashSetLong.size

      override fun contains(element: Long): Boolean {
        return this@ConcurrentHashSetLong.contains(element)
      }

      override fun containsAll(elements: Collection<Long>): Boolean {
        return this@ConcurrentHashSetLong.retainAll(elements)
      }

      override fun isEmpty(): Boolean {
        return this@ConcurrentHashSetLong.isEmpty()
      }
    }
  }
}
