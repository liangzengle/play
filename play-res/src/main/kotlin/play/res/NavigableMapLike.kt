package play.res

import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.Lists
import org.eclipse.collections.api.map.primitive.IntIntMap
import org.eclipse.collections.api.map.primitive.LongIntMap
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap
import play.util.unsafeCast
import java.util.*
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction

/**
 * @author LiangZengle
 */
internal sealed class NavigableMapLike<K, V> {
  abstract fun containsKey(key: K): Boolean
  abstract operator fun get(key: K): V?
  abstract fun first(): V?
  abstract fun last(): V?
  abstract fun higherValue(key: K): V?
  abstract fun lowerValue(key: K): V?
  abstract fun lowerOrEqualValue(key: K): V?
  abstract fun higherOrEqualValue(key: K): V?
  abstract fun slice(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Iterable<V>
}

internal class NavigableIntMap<T>(private val list: List<T>, private val keyMapper: ToIntFunction<T>) :
  NavigableMapLike<Int, T>() {
  private val indexMap: IntIntMap

  init {
    val map = IntIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(keyMapper.applyAsInt(elem), index)
    }
    indexMap = map
  }

  private fun indexOf(key: Int): Int = indexMap.getIfAbsent(key, -1)

  override fun containsKey(key: Int): Boolean {
    return indexMap.containsKey(key)
  }

  override fun get(key: Int): T? {
    val index = indexOf(key)
    if (index == -1) {
      return null
    }
    return list[index]
  }

  override fun first(): T? {
    return list.firstOrNull()
  }

  override fun last(): T? {
    return list.lastOrNull()
  }

  override fun higherValue(key: Int): T? {
    val index = indexOf(key)
    if (index != -1) {
      return if (index == list.size - 1) null else list[index + 1]
    }
    return higherValue0(key)
  }

  private fun higherValue0(notExistsKey: Int): T? {
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { keyMapper.applyAsInt(it) }, notExistsKey)
    val idx = -(insertPoint + 1)
    // 比最大的大
    if (idx == list.size) {
      return null
    }
    // 仅比最大的小
    if (idx == list.size - 1) {
      return list.last()
    }
    // 比最小的小
    if (idx <= 0) {
      return list.first()
    }
    return list[idx]
  }

  override fun lowerValue(key: Int): T? {
    val index = indexOf(key)
    if (index != -1) {
      return if (index == 0) null else list[index - 1]
    }
    return lowerValue0(key)
  }

  private fun lowerValue0(notExistsKey: Int): T? {
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { keyMapper.applyAsInt(it) }, notExistsKey)
    val idx = -(insertPoint + 1)
    // 比最小的小
    if (idx <= 0) {
      return null
    }
    // 比最大的大
    if (idx >= list.size) {
      return list.last()
    }
    return list[idx - 1]
  }

  override fun lowerOrEqualValue(key: Int): T? {
    val index = indexOf(key)
    if (index != -1) {
      return list[index]
    }
    return lowerValue0(key)
  }

  override fun higherOrEqualValue(key: Int): T? {
    val index = indexOf(key)
    if (index != -1) {
      return list[index]
    }
    return higherValue0(key)
  }

  override fun slice(from: Int, fromInclusive: Boolean, to: Int, toInclusive: Boolean): List<T> {
    val ordered = Lists.transform(list) { keyMapper.applyAsInt(it) }
    var fromIndex = indexOf(from)
    if (fromIndex == -1) {
      val insertPoint = Collections.binarySearch(ordered, from)
      fromIndex = -(insertPoint + 1)
    } else {
      if (!fromInclusive) {
        fromIndex += 1
      }
    }

    var toIndex = indexOf(to)
    if (toIndex == -1) {
      val insertPoint = Collections.binarySearch(ordered, to)
      toIndex = -(insertPoint + 1)
    } else {
      if (toInclusive) {
        toIndex += 1
      }
    }
    if (fromIndex >= toIndex) {
      return emptyList()
    }
    return list.subList(fromIndex, toIndex)
  }
}

internal class NavigableLongMap<T>(
  private val list: List<T>,
  private val keyMapper: ToLongFunction<T>
) : NavigableMapLike<Long, T>() {
  private val indexMap: LongIntMap

  init {
    val map = LongIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(keyMapper.applyAsLong(elem), index)
    }
    indexMap = map
  }

  private fun indexOf(key: Long): Int = indexMap.getIfAbsent(key, -1)

  override fun containsKey(key: Long): Boolean {
    return indexMap.containsKey(key)
  }

  override fun get(key: Long): T? {
    val index = indexOf(key)
    if (index == -1) {
      return null
    }
    return list[index]
  }

  override fun first(): T? {
    return list.firstOrNull()
  }

  override fun last(): T? {
    return list.lastOrNull()
  }

  override fun higherValue(key: Long): T? {
    val index = indexOf(key)
    if (index != -1) {
      return if (index == list.size - 1) null else list[index + 1]
    }
    return higherValue0(key)
  }

  private fun higherValue0(notExistsKey: Long): T? {
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { keyMapper.applyAsLong(it) }, notExistsKey)
    val idx = -(insertPoint + 1)
    // 比最大的大
    if (idx == list.size) {
      return null
    }
    // 仅比最大的小
    if (idx == list.size - 1) {
      return list.last()
    }
    // 比最小的小
    if (idx <= 0) {
      return list.first()
    }
    return list[idx]
  }

  override fun lowerValue(key: Long): T? {
    val index = indexOf(key)
    if (index != -1) {
      return if (index == 0) null else list[index - 1]
    }
    return lowerValue0(key)
  }

  private fun lowerValue0(notExistsKey: Long): T? {
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { keyMapper.applyAsLong(it) }, notExistsKey)
    val idx = -(insertPoint + 1)
    // 比最小的小
    if (idx <= 0) {
      return null
    }
    // 比最大的大
    if (idx >= list.size) {
      return list.last()
    }
    return list[idx - 1]
  }

  override fun lowerOrEqualValue(key: Long): T? {
    val index = indexOf(key)
    if (index != -1) {
      return list[index]
    }
    return lowerValue0(key)
  }

  override fun higherOrEqualValue(key: Long): T? {
    val index = indexOf(key)
    if (index != -1) {
      return list[index]
    }
    return higherValue0(key)
  }

  override fun slice(from: Long, fromInclusive: Boolean, to: Long, toInclusive: Boolean): List<T> {
    val ordered = Lists.transform(list) { keyMapper.applyAsLong(it) }
    var fromIndex = indexOf(from)
    if (fromIndex == -1) {
      val insertPoint = Collections.binarySearch(ordered, from)
      fromIndex = -(insertPoint + 1)
    } else {
      if (!fromInclusive) {
        fromIndex += 1
      }
    }

    var toIndex = indexOf(to)
    if (toIndex == -1) {
      val insertPoint = Collections.binarySearch(ordered, to)
      toIndex = -(insertPoint + 1)
    } else {
      if (toInclusive) {
        toIndex += 1
      }
    }
    if (fromIndex >= toIndex) {
      return emptyList()
    }
    return list.subList(fromIndex, toIndex)
  }
}

internal class NavigableRefMap<K : Comparable<*>, T>(list: List<T>, keyMapper: (T) -> K) :
  NavigableMapLike<K, T>() {
  private val sortedMap: NavigableMap<K, T> = if (list.isEmpty()) {
    Collections.emptyNavigableMap()
  } else {
    val b = ImmutableSortedMap.naturalOrder<K, T>()
    for (elem in list) {
      b.put(keyMapper(elem), elem)
    }
    b.build()
  }

  companion object {
    private val Empty = NavigableRefMap<Int, Int>(emptyList()) { it }
    fun <K : Comparable<*>, T> empty(): NavigableRefMap<K, T> = Empty.unsafeCast()
  }

  override fun containsKey(key: K): Boolean {
    return sortedMap.containsKey(key)
  }

  override fun get(key: K): T? {
    return sortedMap[key]
  }

  override fun first(): T? {
    return sortedMap.firstEntry()?.value
  }

  override fun last(): T? {
    return sortedMap.lastEntry()?.value
  }

  override fun higherValue(key: K): T? {
    return sortedMap.higherEntry(key)?.value
  }

  override fun lowerValue(key: K): T? {
    return sortedMap.lowerEntry(key)?.value
  }

  override fun lowerOrEqualValue(key: K): T? {
    return sortedMap.floorEntry(key)?.value
  }

  override fun higherOrEqualValue(key: K): T? {
    return sortedMap.ceilingEntry(key)?.value
  }

  override fun slice(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Collection<T> {
    return sortedMap.subMap(from, fromInclusive, to, toInclusive).values
  }
}
