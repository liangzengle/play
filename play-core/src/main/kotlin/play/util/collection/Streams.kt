package play.util.collection

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import play.util.unsafeCast
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

fun <K, V> Stream<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
  return collect(Collectors.toMap(Pair<K, V>::first, Pair<K, V>::second))
}

fun <T, K> Stream<T>.toMutableMap(keyMapper: (T) -> K): MutableMap<K, T> {
  return collect(Collectors.toMap(keyMapper, Function.identity()))
}

fun <T, K, V> Stream<T>.toMutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): MutableMap<K, V> {
  return collect(Collectors.toMap(keyMapper, valueMapper))
}

fun <K, V> Stream<Pair<K, V>>.toImmutableMap(): Map<K, V> {
  return toImmutableMap(Pair<K, V>::first, Pair<K, V>::second)
}

fun <K, T> Stream<T>.toImmutableMap(keyMapper: (T) -> K): Map<K, T> {
  return collect(ImmutableMap.toImmutableMap(keyMapper, Function.identity()))
}

fun <T, K, V> Stream<T>.toImmutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
  return collect(ImmutableMap.toImmutableMap(keyMapper, valueMapper))
}

fun <T> Stream<T>.toImmutableSet(): ImmutableSet<T> {
  return collect(ImmutableSet.toImmutableSet())
}

fun <T> Stream<T>.toMutableSet(): MutableSet<T> {
  return collect(Collectors.toSet())
}

fun <T> Stream<T>.toImmutableList(): ImmutableList<T> {
  return collect(ImmutableList.toImmutableList())
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Stream<T?>.filterNotNull(): Stream<T> {
  return filter { it != null }.unsafeCast()
}
