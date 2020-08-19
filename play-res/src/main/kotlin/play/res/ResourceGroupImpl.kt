package play.res

import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap
import play.util.reflect.Reflect
import play.util.unsafeCast
import kotlin.reflect.KClass

class ResourceGroupImpl<T : AbstractResource, K : Comparable<K>>(
  resourceClass: Class<T>,
  private val list: List<T>
) : ResourceGroup<T>, UniqueKeyResourceSet<K, T>, NavigableResourceSet<K, T> {

  private val navigator: ResourceSetNavigator<K, T>?

  private val indexMap: IndexMap<K>?

  init {
    val uniqueKeyClass = GroupedWithUniqueKey::class.java
    val keyType: KClass<*>? = if (uniqueKeyClass.isAssignableFrom(resourceClass)) {
      (Reflect.getTypeArg(resourceClass.asSubclass(uniqueKeyClass), uniqueKeyClass, 1) as Class<*>).kotlin
    } else null

    if (keyType != null) {
      val map: NavigableMapLike<K, T> = when (keyType) {
        Int::class -> NavigableIntMap(list) { it.id }
        Long::class -> NavigableLongMap(list) { it.id.toLong() }
        else -> NavigableRefMap(list) {
          it.unsafeCast<GroupedWithUniqueKey<*, *>>().keyInGroup().unsafeCast<Comparable<K>>()
        }
      }.unsafeCast()
      navigator = ResourceSetNavigatorImpl(map)

      val indexMap: IndexMap<K> = when (keyType) {
        Int::class -> IndexMap.IntKey()
        Long::class -> IndexMap.LongKey()
        else -> IndexMap.RefKey()
      }.unsafeCast()
      for (i in list.indices) {
        val key = list[i].unsafeCast<GroupedWithUniqueKey<*, K>>().keyInGroup()
        indexMap.put(key, i)
      }
      this.indexMap = indexMap
    } else {
      navigator = null
      indexMap = null
    }
  }

  private fun indexOf(key: K): Int {
    return indexMap!!.get(key)
  }

  override fun containsKey(key: K): Boolean {
    if (indexMap == null) throw UnsupportedOperationException()
    return indexOf(key) != -1
  }

  override fun getByKeyOrNull(key: K): T? {
    if (indexMap == null) throw UnsupportedOperationException()
    val idx = indexOf(key)
    return if (idx == -1) null else list[idx]
  }

  override fun navigator(): ResourceSetNavigator<K, T> {
    return navigator ?: throw UnsupportedOperationException()
  }

  override fun list(): List<T> = list

  private sealed class IndexMap<K> {
    abstract fun get(key: K): Int
    abstract fun put(key: K, value: Int)

    class IntKey : IndexMap<Int>() {
      private val map = IntIntHashMap()
      override fun get(key: Int): Int = map.getIfAbsent(key, -1)
      override fun put(key: Int, value: Int) = map.put(key, value)
    }

    class LongKey : IndexMap<Long>() {
      private val map = LongIntHashMap()
      override fun get(key: Long): Int = map.getIfAbsent(key, -1)
      override fun put(key: Long, value: Int) = map.put(key, value)
    }

    class RefKey : IndexMap<Any>() {
      private val map = ObjectIntHashMap<Any>()
      override fun get(key: Any): Int = map.getIfAbsent(key, -1)
      override fun put(key: Any, value: Int) = map.put(key, value)
    }

  }
}

