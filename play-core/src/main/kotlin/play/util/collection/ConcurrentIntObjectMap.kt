package play.util.collection

import play.util.function.IntObjToObjFunction
import play.util.function.IntToObjFunction

interface ConcurrentIntObjectMap<V> : Iterable<ConcurrentIntObjectMap.Entry<V>> {
  operator fun get(key: Int): V?

  @JvmDefault
  operator fun set(key: Int, value: V) = put(key, value)
  fun getOrDefault(key: Int, defaultValue: V): V
  fun put(key: Int, value: V): V?
  fun putIfAbsent(key: Int, value: V): V?
  fun remove(key: Int): V?
  fun remove(key: Int, value: V): Boolean
  fun computeIfPresent(key: Int, remappingFunction: IntObjToObjFunction<in V, out V?>): V?
  fun computeIfAbsent(key: Int, function: IntToObjFunction<out V?>): V?
  fun compute(key: Int, remappingFunction: IntObjToObjFunction<in V?, out V?>): V?
  fun containsKey(key: Int): Boolean
  fun isEmpty(): Boolean
  fun isNotEmpty(): Boolean
  val size: Int
  val keys: IntIterable
  val values: Iterable<V>
  fun clear()
  interface Entry<V> {
    val key: Int
    val value: V
  }

  companion object {
    @JvmName("create")
    operator fun <V> invoke(): ConcurrentIntObjectMap<V> = ConcurrentIntObjectHashMap()

    @JvmName("create")
    operator fun <V> invoke(initialSize: Int): ConcurrentIntObjectMap<V> = ConcurrentIntObjectHashMap(initialSize)
  }
}
