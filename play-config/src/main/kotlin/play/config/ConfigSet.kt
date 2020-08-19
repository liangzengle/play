package play.config

import io.vavr.control.Option
import javax.annotation.Nullable

interface BasicConfigSet<T> {
  fun contains(id: Int): Boolean

  fun isEmpty(): Boolean = list().isEmpty()

  fun isNotEmpty(): Boolean = list().isNotEmpty()

  fun size(): Int = list().size

  fun get(id: Int): Option<T>

  fun getOrThrow(id: Int): T

  @Nullable
  fun getOrNull(id: Int): T?

  operator fun invoke(id: Int): T = getOrThrow(id)

  fun list(): List<T>

  fun firstOrThrow(): T = list().first()

  fun lastOrThrow(): T = list().last()

  fun first(): Option<T> {
    return if (isEmpty()) io.vavr.kotlin.none() else io.vavr.kotlin.some(firstOrThrow())
  }

  fun last(): Option<T> {
    return if (isEmpty()) io.vavr.kotlin.none() else io.vavr.kotlin.some(lastOrThrow())
  }

  fun sequence(): Sequence<T> = list().asSequence()

  fun reversedSequence(): Sequence<T> = list().asReversed().asSequence()

  fun reversedList(): List<T> = list().asReversed()

  fun iterator(): Iterator<T> = list().iterator()
}

interface UniqueKeyConfigSet<K, T> {
  fun containsKey(key: K): Boolean

  fun getByKey(key: K): Option<T>

  @Nullable
  fun getByKeyOrNull(key: K): T?

  fun getByKeyOrThrow(key: K): T

  fun firstOrThrow(): T

  fun lastOrThrow(): T

  fun first(): Option<T>

  fun last(): Option<T>

  fun nextOrThrow(key: K): T

  fun next(key: K): Option<T>

  fun prevOrThrow(key: K): T

  fun prev(key: K): Option<T>

  fun equalsOrPrevOption(key: K): Option<T>

  fun equalsOrNextOption(key: K): Option<T>

  fun slice(fromInclusive: K, toInclusive: K): Iterable<T> {
    return slice(fromInclusive, true, toInclusive, true)
  }

  fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T>
}

interface ConfigSet<K, T> : BasicConfigSet<T>, UniqueKeyConfigSet<K, T> {
  override fun firstOrThrow(): T {
    return super.firstOrThrow()
  }

  override fun lastOrThrow(): T {
    return super.lastOrThrow()
  }

  override fun first(): Option<T> {
    return super.first()
  }

  override fun last(): Option<T> {
    return super.last()
  }
}

interface ExtensionConfigSet<E : ConfigExtension<T>, T : AbstractConfig> {
  fun extension(): E
}

interface GroupedConfigSet<G, K, T> {
  fun getGroup(groupId: G): Option<ConfigSet<K, T>>

  @Nullable
  fun getGroupOrNull(groupId: G): ConfigSet<K, T>?

  fun getGroupOrThrow(groupId: G): ConfigSet<K, T>

  fun groupMap(): Map<G, ConfigSet<K, T>>

  fun containsGroup(groupId: G): Boolean
}
