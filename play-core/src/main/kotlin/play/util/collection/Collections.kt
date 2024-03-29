@file:Suppress("NOTHING_TO_INLINE", "unused")

package play.util.collection

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import java.util.*
import java.util.function.Function
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

fun <E> Sequence<E>.filterDuplicated(): Map<E, List<E>> {
  return this.iterator().filterDuplicated()
}

fun <E> Iterable<E>.filterDuplicated(): Map<E, List<E>> {
  return this.iterator().filterDuplicated()
}

fun <E> Iterator<E>.filterDuplicated(): Map<E, List<E>> {
  return filterDuplicatedBy { it }
}

fun <E, ID> Sequence<E>.filterDuplicatedBy(identity: (E) -> ID): Map<ID, List<E>> {
  return this.iterator().filterDuplicatedBy(identity)
}

fun <E, ID> Iterable<E>.filterDuplicatedBy(identity: (E) -> ID): Map<ID, List<E>> {
  return this.iterator().filterDuplicatedBy(identity)
}

fun <E, ID> Iterator<E>.filterDuplicatedBy(identity: (E) -> ID): Map<ID, List<E>> {
  val grouped = hashMapOf<ID, MutableList<E>>()
  val result = linkedMapOf<ID, MutableList<E>>()
  for (e in this) {
    val key = identity(e)
    var list: MutableList<E>? = grouped[key]
    if (list == null) {
      list = ArrayList(2)
      grouped[key] = list
    }
    list.add(e)
    if (list.size > 1 && !result.containsKey(key)) {
      result[key] = list
    }
  }
  return result
}

fun <E> Sequence<E>.groupByCounting(): Map<E, Int> {
  return this.iterator().groupByCounting()
}

fun <E> Iterable<E>.groupByCounting(): Map<E, Int> {
  return this.iterator().groupByCounting()
}

fun <E> Iterator<E>.groupByCounting(): Map<E, Int> {
  val map = mutableMapOf<E, Int>()
  for (e in this) {
    map.merge(e, 1, Integer::sum)
  }
  return map
}

fun <T> iteratorOf(elem: T): Iterator<T> = SingletonIterator(elem)

fun <K, V> mapOf(key: K, value: V): Map<K, V> = Collections.singletonMap(key, value)

fun <T> sequenceOf(elem: T): Sequence<T> = Sequence { iteratorOf(elem) }

fun <T> streamOf(elem: T): Stream<T> = Stream.of(elem)

inline fun <T : Any, K : Any> Array<T>.toImmutableMap(keyMapper: (T) -> K): Map<K, T> {
  val builder = ImmutableMap.builder<K, T>()
  for (e in this) {
    builder.put(keyMapper(e), e)
  }
  return builder.build()
}

inline fun <T : Any, K : Any> Iterable<T>.toImmutableMap(keyMapper: (T) -> K): Map<K, T> {
  val builder = ImmutableMap.builder<K, T>()
  for (e in this) {
    builder.put(keyMapper(e), e)
  }
  return builder.build()
}

inline fun <T, K : Any, V : Any> Iterable<T>.toImmutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
  val builder = ImmutableMap.builder<K, V>()
  for (e in this) {
    builder.put(keyMapper(e), valueMapper(e))
  }
  return builder.build()
}

inline fun <T> Array<T>.stream(): Stream<T> = Arrays.stream(this)
inline fun IntArray.stream(): IntStream = Arrays.stream(this)
inline fun LongArray.stream(): LongStream = Arrays.stream(this)
inline fun DoubleArray.stream(): DoubleStream = Arrays.stream(this)

fun <T, K : Enum<K>> Array<T>.toImmutableEnumMap(keyMapper: (T) -> K): Map<K, T> {
  return stream().collect(Maps.toImmutableEnumMap(keyMapper, Function.identity())) as Map<K, T>
}

fun <T, K : Enum<K>> Collection<T>.toImmutableEnumMap(keyMapper: (T) -> K): Map<K, T> {
  return stream().collect(Maps.toImmutableEnumMap(keyMapper, Function.identity())) as Map<K, T>
}

fun <T> Iterable<T>.mkString(
  separator: Char,
  transform: ((T) -> String)? = null
): String {
  val b = StringBuilder()
  var first = true
  for (e in this) {
    if (!first) {
      b.append(separator)
    }
    if (transform == null) b.append(e) else b.append(transform(e))
    first = false
  }
  return b.toString()
}

fun <T> Iterable<T>.mkString(
  separator: Char,
  prefix: Char,
  postfix: Char,
  transform: ((T) -> String)? = null
): String {
  val b = StringBuilder()
  b.append(prefix)
  var first = true
  for (e in this) {
    if (!first) b.append(separator)
    if (transform == null) b.append(e) else b.append(transform(e))
    first = false
  }
  if (postfix != 0.toChar()) {
    b.append(postfix)
  }
  return b.toString()
}
