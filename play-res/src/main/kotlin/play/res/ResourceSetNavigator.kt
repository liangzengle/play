package play.res

import play.util.toOptional
import java.util.*

interface ResourceSetNavigator<K, T : AbstractResource> {
  fun firstOrNull(): T?

  fun lastOrNull(): T?

  fun firstOrThrow(): T = firstOrNull() ?: throw NoSuchElementException()

  fun lastOrThrow(): T = lastOrNull() ?: throw NoSuchElementException()

  fun first(): Optional<T> = firstOrNull().toOptional()

  fun last(): Optional<T> = lastOrNull().toOptional()

  fun nextOrNull(key: K): T?

  fun nextOrThrow(key: K): T = nextOrNull(key) ?: throw NoSuchElementException("current: $key")

  fun next(key: K): Optional<T> = nextOrNull(key).toOptional()

  fun prevOrNull(key: K): T?

  fun prevOrThrow(key: K): T = prevOrNull(key) ?: throw NoSuchElementException("current: $key")

  fun prev(key: K): Optional<T> = prevOrNull(key).toOptional()

  fun prevOrEquals(key: K): Optional<T>

  fun nextOrEquals(key: K): Optional<T>

  fun slice(fromInclusive: K, toInclusive: K): Iterable<T> {
    return slice(fromInclusive, true, toInclusive, true)
  }

  fun slice(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Iterable<T>
}

internal class ResourceSetNavigatorImpl<K, V : AbstractResource>(private val navigableMap: NavigableMapLike<K, V>) :
  ResourceSetNavigator<K, V> {
  override fun firstOrNull(): V? {
    return navigableMap.first()
  }

  override fun lastOrNull(): V? {
    return navigableMap.last()
  }

  override fun nextOrNull(key: K): V? {
    return navigableMap.higherValue(key)
  }

  override fun prevOrNull(key: K): V? {
    return navigableMap.lowerValue(key)
  }

  override fun prevOrEquals(key: K): Optional<V> {
    return navigableMap.lowerOrEqualValue(key).toOptional()
  }

  override fun nextOrEquals(key: K): Optional<V> {
    return navigableMap.higherOrEqualValue(key).toOptional()
  }

  override fun slice(from: K, fromInclusive: Boolean, to: K, toInclusive: Boolean): Iterable<V> {
    return navigableMap.slice(from, fromInclusive, to, toInclusive)
  }
}
