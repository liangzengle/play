package play.util.collection

import com.fasterxml.jackson.annotation.JsonValue
import com.google.common.primitives.Longs
import play.util.EmptyLongArray
import java.util.*
import java.util.stream.LongStream

/**
 *
 * @author LiangZengle
 */
class BitSet internal constructor(@field:JsonValue private var elements: LongArray) {

  constructor() : this(EmptyLongArray)

  fun add(element: Int): Boolean {
    val exists = contains(element)
    val wordIndex = element shr 6
    val diff = wordIndex - elements.size + 1
    if (diff > 0) {
      elements = elements.copyOf(elements.size + diff)
    }
    elements[wordIndex] = elements[wordIndex] or (1L shl element)
    return !exists
  }

  fun remove(element: Int): Boolean {
    val exists = contains(element)
    val wordIndex = element shr 6
    if (elements.size > wordIndex) {
      elements[wordIndex] = elements[wordIndex] and (1L shl element).inv()
    }
    return exists
  }

  fun contains(element: Int): Boolean {
    val wordIndex = element shr 6
    return elements.size > wordIndex && ((elements[wordIndex] and (1L shl element)) != 0L)
  }

  fun clear() {
    elements = EmptyLongArray
  }

  fun toStream(): LongStream {
    return Arrays.stream(toArray())
  }

  fun toList(): List<Long> {
    return Longs.asList(*toArray())
  }

  fun toArray(): LongArray {
    return elements.clone()
  }

  override fun toString(): String {
    return elements.contentToString()
  }
}
