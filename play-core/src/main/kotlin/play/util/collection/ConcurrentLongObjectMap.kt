package play.util.collection

import play.util.function.LongObjToObjFunction
import play.util.function.LongToObjFunction
import java.util.function.BiFunction
import java.util.stream.LongStream
import java.util.stream.Stream

interface ConcurrentLongObjectMap<V> : Iterable<ConcurrentLongObjectMap.Entry<V>> {
  operator fun get(key: Long): V?
  operator fun set(key: Long, value: V) = put(key, value)
  fun getOrDefault(key: Long, defaultValue: V): V
  fun put(key: Long, value: V): V?
  fun putIfAbsent(key: Long, value: V): V?
  fun remove(key: Long): V?
  fun remove(key: Long, value: V): Boolean
  fun computeIfPresent(key: Long, remappingFunction: LongObjToObjFunction<in V, out V?>): V?
  fun computeIfAbsent(key: Long, function: LongToObjFunction<out V>): V
  fun compute(key: Long, remappingFunction: LongObjToObjFunction<in V?, out V?>): V?
  fun replace(key: Long, oldValue: V, newValue: V): Boolean
  fun merge(key: Long, value: V, remappingFunction: BiFunction<in V?, in V, out V?>): V?
  fun containsKey(key: Long): Boolean
  fun isEmpty(): Boolean
  fun isNotEmpty(): Boolean
  val size: Int
  val keys: LongIterable
  fun keysStream(): LongStream
  val values: Iterable<V>
  fun valuesStream(): Stream<V>
  fun clear()
  interface Entry<V> {
    val key: Long
    val value: V
  }

  companion object {
    @JvmName("create")
    operator fun <V> invoke(): ConcurrentLongObjectMap<V> = ConcurrentLongObjectHashMap()

    @JvmName("create")
    operator fun <V> invoke(initialSize: Int): ConcurrentLongObjectMap<V> = ConcurrentLongObjectHashMap(initialSize)
  }
}
