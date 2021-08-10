package play.res

import java.util.*
import javax.annotation.Nonnull
import javax.annotation.Nullable

interface SingletonResourceSet<T> {
  fun get(): T
}

internal class SingletonResourceSetImpl<K, T, G, E>(private val elem: T) :
  SingletonResourceSet<T>,
  SuperResourceSet<K, T, G, E> where T : AbstractResource, E : ResourceExtension<T> {

  override fun indexOf(elem: T): Int {
    return if (elem === this.elem || elem.id == this.elem.id) 0 else throw NoSuchElementException("${elem.javaClass.simpleName}(${elem.id})")
  }

  override fun getByIndexOrNull(idx: Int): T? = if (idx == 0) elem else null

  override fun get(): T = elem

  override fun contains(id: Int): Boolean = elem.id == id

  override fun get(id: Int): Optional<T> = if (id != elem.id) Optional.empty() else Optional.of(elem)

  override fun getOrThrow(id: Int): T {
    if (!contains(id)) throw NoSuchElementException("${elem.javaClass.simpleName}($id)")
    return elem
  }

  @Nonnull
  override fun getOrNull(id: Int): T? {
    return elem
  }

  override fun list(): List<T> = Collections.singletonList(elem)

  override fun containsKey(key: K): Boolean = false

  override fun getByKey(key: K): Optional<T> = throw UnsupportedOperationException()

  override fun getByKeyOrNull(key: K): T? {
    throw UnsupportedOperationException()
  }

  override fun getByKeyOrThrow(key: K): T {
    throw UnsupportedOperationException()
  }

  override fun nextOrThrow(key: K): T {
    throw UnsupportedOperationException()
  }

  override fun next(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun prevOrThrow(key: K): T {
    throw UnsupportedOperationException()
  }

  override fun prev(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun prevOrEqualsOption(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun nextOrEqualsOption(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    throw UnsupportedOperationException()
  }

  override fun extension(): E {
    throw UnsupportedOperationException()
  }

  override fun getGroup(groupId: G): Optional<BasicResourceSet<T>> {
    throw UnsupportedOperationException()
  }

  @Nullable
  override fun getGroupOrNull(groupId: G): BasicResourceSet<T>? {
    throw UnsupportedOperationException()
  }

  override fun getGroupOrThrow(groupId: G): BasicResourceSet<T> {
    throw UnsupportedOperationException()
  }

  override fun groupMap(): Map<G, BasicResourceSet<T>> {
    throw UnsupportedOperationException()
  }

  override fun containsGroup(groupId: G): Boolean {
    throw UnsupportedOperationException()
  }
}
