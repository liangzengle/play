package play.util.collection

import java.util.*
import kotlin.NoSuchElementException

class SingletonIterator<T>(private val value: T) : Iterator<T> {
  private var exhausted = false

  override fun hasNext(): Boolean = exhausted

  override fun next(): T {
    if (exhausted) {
      throw NoSuchElementException()
    }
    exhausted = true
    return value
  }
}

class SingletonIterable<T>(private val value: T) : Iterable<T> {
  override fun iterator(): Iterator<T> = SingletonIterator(value)
}

inline fun <T> T.asSingletonList(): List<T> = Collections.singletonList(this)
