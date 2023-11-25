package play.util.collection

import org.jctools.maps.NonBlockingSetInt
import java.util.*
import java.util.stream.IntStream
import java.util.stream.StreamSupport

class ConcurrentHashSetInt : MutableSetInt {
  @Transient
  private val underlying = NonBlockingSetInt()

  override val size: Int = underlying.size

  override fun add(element: Int): Boolean {
    return underlying.add(element)
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
    underlying.clear()
  }

  override fun iterator(): IntIterator {
    return object : IntIterator {
      @Suppress("UNCHECKED_CAST")
      private val it = underlying.iterator()
      override fun hasNext(): Boolean = it.hasNext()

      override fun next(): Int = it.next()
    }
  }

  override fun stream(): IntStream {
    return StreamSupport.intStream(
      Spliterators.spliterator(iterator().toJava(), size.toLong(), Spliterator.DISTINCT),
      false
    )
  }

  override fun remove(element: Int): Boolean {
    return underlying.remove(element)
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
    return underlying.retainAll(elements)
  }

  override fun retainAll(elements: MutableSetInt): Boolean {
    return underlying.retainAll(elements.toJava())
  }

  override fun contains(element: Int): Boolean {
    return underlying.contains(element)
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

  override fun isEmpty(): Boolean = underlying.isEmpty()

  override fun toJava(): MutableSet<Int> {
    return object : MutableSet<Int> {
      override fun add(element: Int): Boolean {
        return this@ConcurrentHashSetInt.add(element)
      }

      override fun addAll(elements: Collection<Int>): Boolean {
        return this@ConcurrentHashSetInt.addAll(elements)
      }

      override fun clear() {
        return this@ConcurrentHashSetInt.clear()
      }

      override fun iterator(): MutableIterator<Int> {
        return this@ConcurrentHashSetInt.iterator().toJava()
      }

      override fun remove(element: Int): Boolean {
        return this@ConcurrentHashSetInt.remove(element)
      }

      override fun removeAll(elements: Collection<Int>): Boolean {
        return this@ConcurrentHashSetInt.removeAll(elements)
      }

      override fun retainAll(elements: Collection<Int>): Boolean {
        return this@ConcurrentHashSetInt.retainAll(elements)
      }

      override val size: Int = this@ConcurrentHashSetInt.size

      override fun contains(element: Int): Boolean {
        return this@ConcurrentHashSetInt.contains(element)
      }

      override fun containsAll(elements: Collection<Int>): Boolean {
        return this@ConcurrentHashSetInt.containsAll(elements)
      }

      override fun isEmpty(): Boolean {
        return this@ConcurrentHashSetInt.isEmpty()
      }
    }
  }
}
