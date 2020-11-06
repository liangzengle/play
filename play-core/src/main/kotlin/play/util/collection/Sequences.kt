package play.util.collection

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet

fun <K, V> Sequence<Pair<K, V>>.toImmutableMap(): Map<K, V> {
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
