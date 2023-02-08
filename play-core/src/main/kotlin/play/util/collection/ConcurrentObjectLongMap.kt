package play.util.collection

import play.util.function.ObjLongToObjFunction
import play.util.function.ObjToLongFunction
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.LongBinaryOperator

interface ConcurrentObjectLongMap<K> : Iterable<ConcurrentObjectLongMap.Entry<K>> {
  operator fun get(key: K): Long?
  operator fun set(key: K, value: Long): Long? = put(key, value)
  fun getOrDefault(key: K, defaultValue: Long): Long
  fun put(key: K, value: Long): Long?
  fun putIfAbsent(key: K, value: Long): Long?
  fun remove(key: K): Long?
  fun remove(key: K, value: Long): Boolean
  fun computeIfPresent(key: K, remappingFunction: ObjLongToObjFunction<K, Long>): Long?
  fun computeIfAbsent(key: K, function: Function<K, Long?>): Long?
  fun computeIfAbsent(key: K, function: ObjToLongFunction<K>): Long
  fun compute(key: K, remappingFunction: BiFunction<in K, Long?, Long?>): Long?
  fun replace(key: K, oldValue: Long, newValue: Long): Boolean
  fun merge(key: K, value: Long, remappingFunction: LongBinaryOperator): Long
  fun containsKey(key: K): Boolean
  val isEmpty: Boolean
  val isNotEmpty: Boolean
  val size: Int
  val keys: Iterable<K>
  val values: LongIterable
  fun clear()
  interface Entry<K> {
    val key: K
    val value: Long
    operator fun component1() = key
    operator fun component2() = value
  }

  companion object {
    @JvmName("create")
    operator fun <K> invoke(): ConcurrentObjectLongMap<K> = ConcurrentObjectLongHashMap()

    @JvmName("create")
    operator fun <K> invoke(initialSize: Int): ConcurrentObjectLongMap<K> = ConcurrentObjectLongHashMap(initialSize)
  }
}
