package play.util.collection

/**
 * Compare size with [expectedSize]
 *
 * @receiver Iterable<T>
 * @param expectedSize Int
 * @return
 *   -1: size less than [expectedSize]
 *
 *   0: size equals [expectedSize]
 *
 *   1: size greater than [expectedSize]
 */
fun <T> Iterable<T>.sizeCompareTo(expectedSize: Int): Int {
  if (this is Collection<T>) return size.compareTo(expectedSize)
  val iter = iterator()
  var n = 0
  while (iter.hasNext()) {
    iter.next()
    n++
    if (n > expectedSize) {
      return 1
    }
  }
  return n.compareTo(expectedSize)
}

infix fun <T> Iterable<T>.sizeEq(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) == 0
}

infix fun <T> Iterable<T>.sizeGt(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) > 0
}

infix fun <T> Iterable<T>.sizeGe(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) >= 0
}

infix fun <T> Iterable<T>.sizeLt(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) < 0
}

infix fun <T> Iterable<T>.sizeLe(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) <= 0
}
