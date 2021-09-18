package play.res

import com.google.common.collect.ImmutableMap
import org.eclipse.collections.api.map.primitive.IntIntMap
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap
import play.util.*
import play.util.reflect.Reflect
import java.util.*
import javax.annotation.Nullable

internal interface SuperResourceSet<K, T, G, E> :
  ResourceSet<T>,
  ExtensionResourceSet<E, T>,
  GroupedResourceSet<G, T>,
  NavigableResourceSet<K, T>
  where T : AbstractResource, E : ResourceExtension<T>

@Suppress("UNCHECKED_CAST")
internal class ResourceSetImpl<K, T, G, E>(
  private val resourceClass: Class<T>,
  private val list: List<T>,
  isGroupSet: Boolean = false
) : SuperResourceSet<K, T, G, E> where T : AbstractResource, E : ResourceExtension<T> {

  private val isGrouped = Grouped::class.java.isAssignableFrom(resourceClass) && !isGroupSet

  private val extension: E?

  private val indexMap: IntIntMap

  private val navigator: ResourceSetNavigator<K, T> by unsafeLazy {
    val uniqueKeyClass = classOf<UniqueKey<*>>()
    val keyType = if (!uniqueKeyClass.isAssignableFrom(resourceClass)) Int::class
    else (Reflect.getTypeArg(resourceClass.asSubclass(uniqueKeyClass), uniqueKeyClass, 0) as Class<*>).kotlin
    val map: NavigableMapLike<K, T> = when (keyType) {
      Int::class -> NavigableIntMap(list) { it.id }
      Long::class -> NavigableLongMap(list) { it.id.toLong() }
      else -> NavigableRefMap(list) { it.unsafeCast<UniqueKey<*>>().key().unsafeCast<Comparable<K>>() }
    }.unsafeCast()
    ResourceSetNavigatorImpl(map)
  }

  init {
    // init Extension
    val hasExtension = ExtensionKey::class.java.isAssignableFrom(resourceClass) && !isGroupSet
    extension = if (hasExtension) {
      val extensionClass = resourceClass.asSubclass(ExtensionKey::class.java)
        .getTypeArg(ExtensionKey::class.java, 0)
        .getRawClass<E>()
      try {
        val ctor = extensionClass.getDeclaredConstructor(List::class.java)
        ctor.trySetAccessible()
        ctor.newInstance(list)
      } catch (e: NoSuchMethodException) {
        throw IllegalStateException("${extensionClass.simpleName}缺少构造器：${extensionClass.simpleName}(List<${resourceClass.simpleName}>)")
      } catch (e: Exception) {
        throw IllegalStateException("创建ResourceExtension<${resourceClass.simpleName}>失败", e)
      }
    } else null
    // init indexMap
    val map = IntIntHashMap(list.size)
    for (index in list.indices) {
      val elem = list[index]
      map.put(elem.id, index)
    }
    indexMap = map.toImmutable()
  }

  private val groupMap: Map<G, ResourceGroup<T>> by unsafeLazy {
    val map = list
      .groupBy { (it as Grouped<G>).groupBy() }
      .mapValues { ResourceGroupImpl<T, Comparable<*>>(resourceClass, it.value) }
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
    return getOrNull(id) ?: throw NoSuchElementException("${resourceClass.simpleName}($id)")
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

  override fun extension(): E {
    return extension ?: throw NoSuchElementException("${resourceClass.simpleName}.extension")
  }

  override fun navigator(): ResourceSetNavigator<K, T> {
    return navigator
  }

  @Nullable
  override fun getGroupOrNull(groupId: G): ResourceGroup<T>? {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap[groupId]
  }

  override fun groupMap(): Map<G, ResourceGroup<T>> {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap
  }

  override fun containsGroup(groupId: G): Boolean {
    if (!isGrouped) throw UnsupportedOperationException()
    return groupMap.containsKey(groupId)
  }
}
