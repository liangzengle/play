package play.config

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSortedMap
import io.vavr.control.Option
import io.vavr.kotlin.option
import io.vavr.kotlin.some
import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import play.util.reflect.getRawClass
import play.util.reflect.getTypeArg
import java.util.*
import javax.annotation.Nullable
import kotlin.Comparator
import kotlin.NoSuchElementException

internal interface SuperConfigSet<K, T, G, E> : ConfigSet<K, T>, ExtensionConfigSet<E, T>,
  GroupedConfigSet<G, K, T> where T : AbstractConfig, E : ConfigExtension<T>

@Suppress("UNCHECKED_CAST")
internal class ConfigSetImpl<K, T, G, E>(
  private val configClass: Class<T>,
  private val list: List<T>,
  isGroupSet: Boolean = false
) : SuperConfigSet<K, T, G, E> where T : AbstractConfig, E : ConfigExtension<T> {

  private val isGrouped = Grouped::class.java.isAssignableFrom(configClass) && !isGroupSet
  private val hasExtension = ExtensionKey::class.java.isAssignableFrom(configClass) && !isGroupSet

  private val extensionOpt = if (!hasExtension) io.vavr.kotlin.none() else {
    val extensionClass = configClass.asSubclass(ExtensionKey::class.java)
      .getTypeArg(ExtensionKey::class.java, 0)
      .getRawClass<E>()
    try {
      val extension = extensionClass.getDeclaredConstructor(List::class.java)
        .newInstance(list)
      some(extension)
    } catch (e: NoSuchMethodException) {
      throw IllegalStateException("${extensionClass.simpleName}缺少构造器：${extensionClass.simpleName}(List<${configClass.simpleName}>)")
    } catch (e: Exception) {
      throw IllegalStateException("创建ConfigExtension<${configClass.simpleName}>失败", e)
    }
  }


  private val hashMap: IntObjectMap<T> by lazy(LazyThreadSafetyMode.NONE) {
    val map = IntObjectHashMap<T>(list.size)
    list.forEach { map.put(it.id, it) }
    map.toImmutable()
  }

  private val treeMap: NavigableMap<K, T> by lazy(LazyThreadSafetyMode.NONE) {
    if (list.isEmpty()) ImmutableSortedMap.of()
    else {
      val comparator = when (val first = list.first()) {
        is UniqueKey<*> -> first.keyComparator() as Comparator<K>
        else -> Comparator.comparingInt<T> { it.id } as Comparator<K>
      }
      val b = ImmutableSortedMap.orderedBy<K, T>(comparator)
      list.forEach { b.put((it as UniqueKey<K>).key(), it) }
      b.build()
    }
  }


  private val groupMap: Map<G, ConfigSet<K, T>> by lazy(LazyThreadSafetyMode.NONE) {
    assert(isGrouped)
    val map = list
      .groupBy { (it as Grouped<G>).groupId() }
      .mapValues { ConfigSetImpl<K, T, G, E>(configClass, ImmutableList.copyOf(it.value), true) }
    ImmutableMap.copyOf(map)
  }

  override fun contains(id: Int): Boolean {
    return hashMap.containsKey(id)
  }

  override fun get(id: Int): Option<T> {
    return hashMap.get(id).option()
  }

  override fun getOrThrow(id: Int): T {
    return hashMap.get(id) ?: throw NoSuchElementException("${configClass.simpleName}($id)")
  }

  override fun getOrNull(id: Int): T? {
    return hashMap.get(id)
  }

  override fun list(): List<T> {
    return list
  }

  override fun containsKey(key: K): Boolean {
    return treeMap.containsKey(key)
  }

  override fun getByKey(key: K): Option<T> {
    return treeMap[key].option()
  }

  override fun getByKeyOrNull(key: K): T? {
    return treeMap[key]
  }

  override fun getByKeyOrThrow(key: K): T {
    return treeMap[key] ?: throw NoSuchElementException("${configClass.simpleName} key($key)")
  }

  override fun nextOrThrow(key: K): T {
    return treeMap.higherEntry(key)?.value
      ?: throw NoSuchElementException("${configClass.simpleName} key($key).next")
  }

  override fun next(key: K): Option<T> {
    return treeMap.higherEntry(key)?.value.option()
  }

  override fun prevOrThrow(key: K): T {
    return treeMap.lowerEntry(key)?.value
      ?: throw NoSuchElementException("${configClass.simpleName} key($key).prev")
  }

  override fun prev(key: K): Option<T> {
    return treeMap.lowerEntry(key)?.value.option()
  }

  override fun equalsOrPrevOption(key: K): Option<T> {
    return treeMap.floorEntry(key)?.value.option()
  }

  override fun equalsOrNextOption(key: K): Option<T> {
    return treeMap.ceilingEntry(key)?.value.option()
  }

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    return treeMap.subMap(from, fromIncluded, to, toIncluded).values
  }

  override fun extension(): E {
    return extensionOpt.orNull ?: throw NoSuchElementException("${configClass.simpleName}.extension")
  }

  override fun getGroup(groupId: G): Option<ConfigSet<K, T>> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId].option()
  }

  @Nullable
  override fun getGroupOrNull(groupId: G): ConfigSet<K, T>? {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId]
  }

  override fun getGroupOrThrow(groupId: G): ConfigSet<K, T> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId] ?: throw NoSuchElementException("${configClass.simpleName} group($groupId)")
  }

  override fun groupMap(): Map<G, ConfigSet<K, T>> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap
  }

  override fun containsGroup(groupId: G): Boolean {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap.containsKey(groupId)
  }
}
