package play.res

import com.google.common.collect.Lists
import org.eclipse.collections.api.map.primitive.IntIntMap
import org.eclipse.collections.api.map.primitive.LongIntMap
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap
import java.util.*

/**
 * @author LiangZengle
 */
internal sealed class NavigableMapLike<K, V> {
  abstract fun containsKey(key: K): Boolean
  abstract operator fun get(key: K): V?
  abstract fun higherValue(key: K): V?
  abstract fun lowerValue(key: K): V?
  abstract fun lowerOrEqualValue(key: K): V?
  abstract fun higherOrEqualValue(key: K): V?
  abstract fun subMap(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Iterable<V>
}

internal class NavigableIntMap<T : UniqueKey<Int>>(private val list: List<T>) :
  NavigableMapLike<Int, T>() {
  private val indexMap: IntIntMap

  init {
    val map = IntIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(elem.key(), index)
    }
    indexMap = map
  }

  private fun indexOf(key: Int): Int = indexMap.getIfAbsent(key, -1)

  override fun containsKey(key: Int): Boolean {
    return indexMap.containsKey(key)
  }

  override fun get(key: Int): T? {
    val index = indexMap.getIfAbsent(key, -1)
    if (index == -1) {
      return null
    }
    return list[index]
  }

  override fun higherValue(key: Int): T? {
    val index = indexOf(key) + 1
    if (list.size >= index) {
      return null
    }
    return list[index]
  }

  override fun lowerValue(key: Int): T? {
    val index = indexOf(key) - 1
    if (index < 0 || list.size >= index) {
      return null
    }
    return list[index]
  }

  override fun lowerOrEqualValue(key: Int): T? {
    val index = indexOf(key)
    if (index >= 0) {
      return list[index]
    }
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { it!!.key() }, key)
    if (insertPoint >= 0) {
      return list[insertPoint]
    }
    // -x - 1 = y
    val idx = -(insertPoint + 1)
    if (idx == 0) {
      return null
    }
    return list[idx - 1]
  }

  override fun higherOrEqualValue(key: Int): T? {
    val index = indexOf(key)
    if (index >= 0) {
      return list[index]
    }
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { it!!.key() }, key)
    if (insertPoint >= 0) {
      return list[insertPoint]
    }
    val idx = -(insertPoint + 1)
    if (idx == list.size) {
      return null
    }
    return list[insertPoint + 1]
  }

  override fun subMap(from: Int, fromInclusive: Boolean, to: Int, toInclusive: Boolean): Iterable<T> {
    val ordered = Lists.transform(list) { it!!.key() }
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

internal class NavigableLongMap<T : UniqueKey<Long>>(private val list: List<T>) :
  NavigableMapLike<Long, T>() {
  private val indexMap: LongIntMap

  init {
    val map = LongIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(elem.key(), index)
    }
    indexMap = map
  }

  private fun indexOf(key: Long): Int = indexMap.getIfAbsent(key, -1)

  override fun containsKey(key: Long): Boolean {
    return indexMap.containsKey(key)
  }

  override fun get(key: Long): T? {
    val index = indexMap.getIfAbsent(key, -1)
    if (index == -1) {
      return null
    }
    return list[index]
  }

  override fun higherValue(key: Long): T? {
    val index = indexOf(key)
    if (index == -1 || index == list.size - 1) {
      return null
    }
    return list[index + 1]
  }

  override fun lowerValue(key: Long): T? {
    val index = indexOf(key)
    if (index <= 0) {
      return null
    }
    return list[index]
  }

  override fun lowerOrEqualValue(key: Long): T? {
    val index = indexOf(key)
    if (index >= 0) {
      return list[index]
    }
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { it!!.key() }, key)
    if (insertPoint >= 0) {
      return list[insertPoint]
    }
    // -x - 1 = y
    val idx = -(insertPoint + 1)
    if (idx == 0) {
      return null
    }
    return list[idx - 1]
  }

  override fun higherOrEqualValue(key: Long): T? {
    val index = indexOf(key)
    if (index >= 0) {
      return list[index]
    }
    val insertPoint =
      Collections.binarySearch(Lists.transform(list) { it!!.key() }, key)
    if (insertPoint >= 0) {
      return list[insertPoint]
    }
    val idx = -(insertPoint + 1)
    if (idx == list.size) {
      return null
    }
    return list[insertPoint + 1]
  }

  override fun subMap(from: Long, fromInclusive: Boolean, to: Long, toInclusive: Boolean): Iterable<T> {
    val ordered = Lists.transform(list) { it!!.key() }
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

internal class NavigableRefMap<K, V : UniqueKey<K>>(list: List<V>) : NavigableMapLike<K, V>() {
  private val treeMap: NavigableMap<K, V> = if (list.isEmpty()) {
    Collections.emptyNavigableMap()
  } else {
    val treeMap = TreeMap<K, V>(list.first().keyComparator())
    for (elem in list) {
      treeMap[elem.key()] = elem
    }
    treeMap
  }

  override fun containsKey(key: K): Boolean {
    return treeMap.containsKey(key)
  }

  override fun get(key: K): V? {
    return treeMap[key]
  }

  override fun higherValue(key: K): V? {
    return treeMap.higherEntry(key)?.value
  }

  override fun lowerValue(key: K): V? {
    return treeMap.lowerEntry(key)?.value
  }

  override fun lowerOrEqualValue(key: K): V? {
    return treeMap.floorEntry(key)?.value
  }

  override fun higherOrEqualValue(key: K): V? {
    return treeMap.ceilingEntry(key).value
  }

  override fun subMap(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Iterable<V> {
    return treeMap.subMap(from, fromInclusive, to, toInclusive).values
  }
}
