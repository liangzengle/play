package play.util.collection

import play.util.function.LongLongToObjFunction
import play.util.function.LongObjToObjFunction
import play.util.function.LongToLongFunction
import play.util.function.LongToObjFunction
import java.util.function.LongBinaryOperator

interface ConcurrentLongLongMap : Iterable<ConcurrentLongLongMap.Entry> {

  operator fun get(key: Long): Long?

  fun getOrDefault(key: Long, defaultValue: Long): Long

  fun put(key: Long, value: Long): Long?

  fun putIfAbsent(key: Long, value: Long): Long?

  fun remove(key: Long): Long?

  fun remove(key: Long, value: Long): Boolean

  fun computeIfPresent(key: Long, remappingFunction: LongLongToObjFunction<Long?>): Long?

  fun computeIfAbsent(key: Long, remappingFunction: LongToObjFunction<Long?>): Long?

  fun computeIfAbsent(key: Long, remappingFunction: LongToLongFunction): Long

  fun compute(key: Long, remappingFunction: LongObjToObjFunction<Long?, Long?>): Long?

  fun replace(key: Long, oldValue: Long, newValue: Long): Boolean

  fun merge(key: Long, value: Long, remappingFunction: LongBinaryOperator): Long

  fun containsKey(key: Long): Boolean

  fun isEmpty(): Boolean

  fun isNotEmpty(): Boolean

  fun size(): Int

  fun keys(): LongIterable

  fun values(): LongIterable

  fun entries(): Iterable<Entry>

  fun clear()

  interface Entry {
    val key: Long
    val value: Long
  }

  companion object {
    @JvmStatic
    @JvmName("create")
    operator fun invoke(): ConcurrentLongLongMap = ConcurrentLongLongHashMap()

    @JvmStatic
    @JvmName("create")
    operator fun invoke(initialCapacity: Int): ConcurrentLongLongMap = ConcurrentLongLongHashMap(initialCapacity)
  }
}
