package play.res

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.eclipse.collections.api.map.primitive.IntIntMap
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap
import play.util.getRawClass
import play.util.getTypeArg
import play.util.unsafeCast
import play.util.unsafeLazy
import java.util.*
import javax.annotation.Nullable

internal interface SuperResourceSet<K, T, G, E> :
  ResourceSet<K, T>,
  ExtensionResourceSet<E, T>,
  GroupedResourceSet<G, T> where T : AbstractResource, E : ResourceExtension<T>

@Suppress("UNCHECKED_CAST")
internal class ResourceSetImpl<K, T, G, E>(
  private val configClass: Class<T>,
  private val list: List<T>,
  isGroupSet: Boolean = false
) : SuperResourceSet<K, T, G, E> where T : AbstractResource, E : ResourceExtension<T> {

  private val isGrouped = Grouped::class.java.isAssignableFrom(configClass) && !isGroupSet
  private val hasExtension = ExtensionKey::class.java.isAssignableFrom(configClass) && !isGroupSet

  private val extensionOpt = if (!hasExtension) Optional.empty() else {
    val extensionClass = configClass.asSubclass(ExtensionKey::class.java)
      .getTypeArg(ExtensionKey::class.java, 0)
      .getRawClass<E>()
    try {
      val ctor = extensionClass.getDeclaredConstructor(List::class.java)
      ctor.trySetAccessible()
      val extension = ctor.newInstance(list)
      Optional.of(extension)
    } catch (e: NoSuchMethodException) {
      throw IllegalStateException("${extensionClass.simpleName}缺少构造器：${extensionClass.simpleName}(List<${configClass.simpleName}>)")
    } catch (e: Exception) {
      throw IllegalStateException("创建ConfigExtension<${configClass.simpleName}>失败", e)
    }
  }

  private val indexMap: IntIntMap = run {
    val map = IntIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(elem.id, index)
    }
    map.toImmutable()
  }

  private val nav: NavigableMapLike<K, T> by unsafeLazy {
    val list = this.list.unsafeCast<List<UniqueKey<K>>>()
    val map = if (list.isEmpty()) NavigableRefMap(list)
    else {
      when (list.first().unsafeCast<UniqueKey<K>>()::class) {
        Int::class -> NavigableIntMap(list.unsafeCast<List<UniqueKey<Int>>>())
        Long::class -> NavigableLongMap(list.unsafeCast<List<UniqueKey<Long>>>())
        else -> NavigableRefMap(list.unsafeCast<List<UniqueKey<K>>>())
      }
    }
    map.unsafeCast()
  }

  private val groupMap: Map<G, ResourceSet<K, T>> by unsafeLazy {
    assert(isGrouped)
    val map = list
      .groupBy { (it as Grouped<G>).groupId() }
      .mapValues { ResourceSetImpl<K, T, G, E>(configClass, ImmutableList.copyOf(it.value), true) }
    ImmutableMap.copyOf(map)
  }

  override fun indexOf(elem: T): Int {
    val id = elem.id
    val idx = indexMap.getIfAbsent(id, -1)
    if (idx == -1) {
      throw NoSuchElementException("${elem.javaClass.simpleName}($id)")
    }
    return idx
  }

  override fun getByIndexOrNull(idx: Int): T? {
    if (idx < 0 || idx >= list.size) {
      return null
    }
    return list[idx]
  }

  override fun contains(id: Int): Boolean {
    return indexMap.containsKey(id)
  }

  override fun get(id: Int): Optional<T> {
    return Optional.ofNullable(getOrNull(id))
  }

  override fun getOrThrow(id: Int): T {
    return getOrNull(id) ?: throw NoSuchElementException("${configClass.simpleName}($id)")
  }

  override fun getOrNull(id: Int): T? {
    val index = indexMap.getIfAbsent(id, -1)
    if (index == -1) {
      return null
    }
    return list[index]
  }

  override fun list(): List<T> {
    return list
  }

  override fun containsKey(key: K): Boolean {
    return nav.containsKey(key)
  }

  override fun getByKey(key: K): Optional<T> {
    return Optional.ofNullable(nav[key])
  }

  override fun getByKeyOrNull(key: K): T? {
    return nav[key]
  }

  override fun getByKeyOrThrow(key: K): T {
    return nav[key] ?: throw NoSuchElementException("${configClass.simpleName} key($key)")
  }

  override fun nextOrThrow(key: K): T {
    return nav.higherValue(key)
      ?: throw NoSuchElementException("${configClass.simpleName} key($key).next")
  }

  override fun next(key: K): Optional<T> {
    return Optional.ofNullable(nextOrThrow(key))
  }

  override fun prevOrThrow(key: K): T {
    return nav.lowerValue(key)
      ?: throw NoSuchElementException("${configClass.simpleName} key($key).prev")
  }

  override fun prev(key: K): Optional<T> {
    return Optional.ofNullable(prevOrThrow(key))
  }

  override fun prevOrEqualsOption(key: K): Optional<T> {
    return Optional.ofNullable(nav.lowerOrEqualValue(key))
  }

  override fun nextOrEqualsOption(key: K): Optional<T> {
    return Optional.ofNullable(nav.higherOrEqualValue(key))
  }

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    return nav.subMap(from, fromIncluded, to, toIncluded)
  }

  override fun extension(): E {
    return extensionOpt.orElseThrow { throw NoSuchElementException("${configClass.simpleName}.extension") }
  }

  override fun getGroup(groupId: G): Optional<BasicResourceSet<T>> {
    if (!isGrouped) throw UnsupportedOperationException()
    return Optional.ofNullable(groupMap[groupId])
  }

  @Nullable
  override fun getGroupOrNull(groupId: G): BasicResourceSet<T>? {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId]
  }

  override fun getGroupOrThrow(groupId: G): BasicResourceSet<T> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId] ?: throw NoSuchElementException("${configClass.simpleName} group($groupId)")
  }

  override fun groupMap(): Map<G, BasicResourceSet<T>> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap
  }

  override fun containsGroup(groupId: G): Boolean {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap.containsKey(groupId)
  }
}
