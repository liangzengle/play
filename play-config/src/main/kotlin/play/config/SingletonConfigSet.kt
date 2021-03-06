package play.config

import java.util.*
import javax.annotation.Nonnull
import javax.annotation.Nullable
import kotlin.NoSuchElementException

interface SingletonConfigSet<T> {
  fun get(): T
}

internal class SingletonConfigSetImpl<K, T, G, E>(private val elem: T) :
  SingletonConfigSet<T>,
  SuperConfigSet<K, T, G, E> where T : AbstractConfig, E : ConfigExtension<T> {

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

  override fun equalsOrPrevOption(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun equalsOrNextOption(key: K): Optional<T> {
    throw UnsupportedOperationException()
  }

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    throw UnsupportedOperationException()
  }

  override fun extension(): E {
    throw UnsupportedOperationException()
  }

  override fun getGroup(groupId: G): Optional<ConfigSet<K, T>> {
    throw UnsupportedOperationException()
  }

  @Nullable
  override fun getGroupOrNull(groupId: G): ConfigSet<K, T>? {
    throw UnsupportedOperationException()
  }

  override fun getGroupOrThrow(groupId: G): ConfigSet<K, T> {
    throw UnsupportedOperationException()
  }

  override fun groupMap(): Map<G, ConfigSet<K, T>> {
    throw UnsupportedOperationException()
  }

  override fun containsGroup(groupId: G): Boolean {
    throw UnsupportedOperationException()
  }
}
