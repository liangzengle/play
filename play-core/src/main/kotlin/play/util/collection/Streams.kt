package play.util.collection

import com.google.common.collect.ImmutableMap
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

fun <K, V> Stream<Pair<K, V>>.toMap(): Map<K, V> {
  return toMutableMap()
}

fun <K, V> Stream<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
  return collect(Collectors.toMap({ it!!.first }, { it!!.second }))
}

fun <K, V> Stream<Pair<K, V>>.toImmutableMap(): Map<K, V> {
  return collect(
    Collector.of<Pair<K, V>, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>>(
      { ImmutableMap.builder() },
      { builder, pair -> builder.put(pair.first, pair.second) },
      { b1, b2 ->
        b1.putAll(b2.build())
        b1
      },
      { it.build() },
      Collector.Characteristics.UNORDERED
    )
  )
}

fun <T, K, V> Stream<T>.toImmutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
  return collect(
    Collector.of<T, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>>(
      { ImmutableMap.builder() },
      { builder, elem -> builder.put(keyMapper(elem), valueMapper(elem)) },
      { b1, b2 ->
        b1.putAll(b2.build())
        b1
      },
      { it.build() },
      Collector.Characteristics.UNORDERED
    )
  )
}

fun <K, T> Stream<T>.toImmutableMap(keyMapper: (T) -> K): Map<K, T> {
  return collect(
    Collector.of<T, ImmutableMap.Builder<K, T>, ImmutableMap<K, T>>(
      { ImmutableMap.builder() },
      { builder, elem -> builder.put(keyMapper(elem), elem) },
      { b1, b2 ->
        b1.putAll(b2.build())
        b1
      },
      { it.build() },
      Collector.Characteristics.UNORDERED
    )
  )
}

fun IntStream.toImmutableList(): IntArrayList {
  return ImmutableIntArrayList.wrapOf(toArray())
}
