package play.res

import play.util.toOptional
import play.util.unsafeCast
import java.util.*
import javax.annotation.Nullable

interface ResourceSet<T : AbstractResource> {
  fun indexOf(elem: T): Int

  fun getByIndexOrNull(idx: Int): T?

  fun getByIndex(idx: Int): Optional<T> {
    return Optional.ofNullable(getByIndexOrNull(idx))
  }

  fun contains(id: Int): Boolean

  fun isEmpty(): Boolean = list().isEmpty()

  fun isNotEmpty(): Boolean = list().isNotEmpty()

  fun size(): Int = list().size

  fun get(id: Int): Optional<T> = getOrNull(id).toOptional()

  fun getOrThrow(id: Int): T = getOrNull(id) ?: throw NoSuchElementException("$id")

  fun getOrNull(id: Int): T?

  operator fun invoke(id: Int): T = getOrThrow(id)

  fun list(): List<T>

  fun reversedList(): List<T> = list().asReversed()

  fun firstOrThrow(): T = list().first()

  fun lastOrThrow(): T = list().last()

  fun firstOrNull(): T? = if (isEmpty()) null else list().first()

  fun lastOrNull(): T? = if (isEmpty()) null else list().last()

  fun first(): Optional<T> = Optional.ofNullable(firstOrNull())

  fun last(): Optional<T> = Optional.ofNullable(lastOrNull())
}

internal class EmptyResourceSet<T : AbstractResource> : ResourceSet<T> {
  companion object {
    private val Empty = EmptyResourceSet<AbstractResource>()
    fun <T : AbstractResource> of(): ResourceSet<T> = Empty.unsafeCast()
  }

  override fun indexOf(elem: T): Int = -1
  override fun getByIndexOrNull(idx: Int): T? = null
  override fun contains(id: Int): Boolean = false
  override fun getOrNull(id: Int): T? = null
  override fun list(): List<T> = emptyList()
}


interface UniqueKeyResourceSet<K, T> {
  fun containsKey(key: K): Boolean

  fun getByKey(key: K): Optional<T> = getByKeyOrNull(key).toOptional()

  fun getByKeyOrNull(key: K): T?

  fun getByKeyOrThrow(key: K): T = getByKeyOrNull(key) ?: throw NoSuchElementException("$key")
}

interface NavigableResourceSet<K, T : AbstractResource> {
  fun navigator(): ResourceSetNavigator<K, T>
}

interface ExtensionResourceSet<E : ResourceExtension<T>, T : AbstractResource> {
  fun extension(): E
}

interface GroupedResourceSet<G, T : AbstractResource> {
  fun getGroup(groupId: G): Optional<out ResourceGroup<T>> = getGroupOrNull(groupId).toOptional()

  @Nullable
  fun getGroupOrNull(groupId: G): ResourceGroup<T>?

  fun getGroupOrThrow(groupId: G): ResourceGroup<T> =
    getGroupOrNull(groupId) ?: throw NoSuchElementException("group: $groupId")

  fun groupMap(): Map<G, ResourceGroup<T>>

  fun containsGroup(groupId: G): Boolean
}

interface UniqueKeyResourceGroup<T : AbstractResource, K : Comparable<K>>
  : ResourceGroup<T>, UniqueKeyResourceSet<K, T>, NavigableResourceSet<K, T>

interface ResourceGroup<T : AbstractResource> {
  fun list(): List<T>

  fun reversedList(): List<T> = list().asReversed()
}
