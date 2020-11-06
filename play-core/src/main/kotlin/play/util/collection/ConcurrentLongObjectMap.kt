package play.util.collection

import play.util.function.LongObjToObjFunction
import play.util.function.LongToObjFunction

interface ConcurrentLongObjectMap<V> : Iterable<ConcurrentLongObjectMap.Entry<V>> {
  operator fun get(key: Long): V?

  @JvmDefault
  operator fun set(key: Long, value: V) = put(key, value)
  fun getOrDefault(key: Long, defaultValue: V): V
  fun put(key: Long, value: V): V?
  fun putIfAbsent(key: Long, value: V): V?
  fun remove(key: Long): V?
  fun remove(key: Long, value: V): Boolean
  fun computeIfPresent(key: Long, remappingFunction: LongObjToObjFunction<in V, out V?>): V?
  fun computeIfAbsent(key: Long, function: LongToObjFunction<out V?>): V?
  fun compute(key: Long, remappingFunction: LongObjToObjFunction<in V?, out V?>): V?
  fun containsKey(key: Long): Boolean
  fun isEmpty(): Boolean
  fun isNotEmpty(): Boolean
  val size: Int
  val keys: LongIterable
  val values: Iterable<V>
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
