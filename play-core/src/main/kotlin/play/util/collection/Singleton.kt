package play.util.collection

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
