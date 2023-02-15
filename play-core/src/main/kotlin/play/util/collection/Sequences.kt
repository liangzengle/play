package play.util.collection

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet

fun <K : Any, V : Any> Sequence<Pair<K, V>>.toImmutableMap(): Map<K, V> {
  val builder = ImmutableMap.builder<K, V>()
  forEach { builder.put(it.first, it.second) }
  return builder.build()
}

fun <T> Sequence<T>.toImmutableSet(): Set<T> {
  val builder = ImmutableSet.builder<T>()
  forEach { builder.add(it) }
  return builder.build()
}

fun <T> Sequence<T>.toImmutableList(): List<T> {
  val builder = ImmutableList.builder<T>()
  forEach { builder.add(it) }
  return builder.build()
}

fun <T> Sequence<T>.sizeCompareTo(expectedSize: Int): Int {
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

infix fun <T> Sequence<T>.sizeEq(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) == 0
}

infix fun <T> Sequence<T>.sizeGt(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) > 0
}

infix fun <T> Sequence<T>.sizeGe(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) >= 0
}

infix fun <T> Sequence<T>.sizeLt(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) < 0
}

infix fun <T> Sequence<T>.sizeLe(expectedSize: Int): Boolean {
  return sizeCompareTo(expectedSize) <= 0
}
