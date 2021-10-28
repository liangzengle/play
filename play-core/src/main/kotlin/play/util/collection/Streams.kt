package play.util.collection

import com.google.common.collect.ImmutableMap
import play.util.unsafeCast
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

fun <K, V> Stream<Pair<K, V>>.toMap(): Map<K, V> {
  return toImmutableMap()
}

fun <K, V> Stream<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
  return collect(Collectors.toMap(Pair<K, V>::first, Pair<K, V>::second))
}

fun <K, V> Stream<Pair<K, V>>.toImmutableMap(): Map<K, V> {
  return toImmutableMap(Pair<K, V>::first, Pair<K, V>::second)
}

fun <T, K, V> Stream<T>.toImmutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
  return collect(ImmutableMap.toImmutableMap(keyMapper, valueMapper))
}

fun <K, T> Stream<T>.toImmutableMap(keyMapper: (T) -> K): Map<K, T> {
  return collect(ImmutableMap.toImmutableMap(keyMapper, Function.identity()))
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Stream<T?>.filterNotNull(): Stream<T> {
  return filter { it != null }.unsafeCast()
}
