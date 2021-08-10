package play.res

import java.util.*
import java.util.stream.Stream
import javax.annotation.Nullable

interface BasicResourceSet<T : Any> {
  fun indexOf(elem: T): Int

  fun getByIndexOrNull(idx: Int): T?

  fun getByIndex(idx: Int): Optional<T> {
    return Optional.ofNullable(getByIndexOrNull(idx))
  }

  fun contains(id: Int): Boolean

  fun isEmpty(): Boolean = list().isEmpty()

  fun isNotEmpty(): Boolean = list().isNotEmpty()

  fun size(): Int = list().size

  fun get(id: Int): Optional<T>

  fun getOrThrow(id: Int): T

  @Nullable
  fun getOrNull(id: Int): T?

  operator fun invoke(id: Int): T = getOrThrow(id)

  fun list(): List<T>

  fun firstOrThrow(): T = list().first()

  fun lastOrThrow(): T = list().last()

  fun firstOrNull(): T? = if (isEmpty()) null else list().first()

  fun lastOrNull(): T? = if (isEmpty()) null else list().last()

  fun first(): Optional<T> = Optional.ofNullable(firstOrNull())

  fun last(): Optional<T> = Optional.ofNullable(lastOrNull())

  fun sequence(): Sequence<T> = list().asSequence()

  fun reversedSequence(): Sequence<T> = list().asReversed().asSequence()

  fun reversedList(): List<T> = list().asReversed()

  fun stream(): Stream<T> = list().stream()

  fun reversedStream(): Stream<T> = list().asReversed().stream()

  fun iterator(): Iterator<T> = list().iterator()
}

interface UniqueKeyResourceSet<K, T> {
  fun containsKey(key: K): Boolean

  fun getByKey(key: K): Optional<T>

  @Nullable
  fun getByKeyOrNull(key: K): T?

  fun getByKeyOrThrow(key: K): T

  fun firstOrThrow(): T

  fun lastOrThrow(): T

  fun first(): Optional<T>

  fun last(): Optional<T>

  fun nextOrThrow(key: K): T

  fun next(key: K): Optional<T>

  fun prevOrThrow(key: K): T

  fun prev(key: K): Optional<T>

  fun prevOrEqualsOption(key: K): Optional<T>

  fun nextOrEqualsOption(key: K): Optional<T>

  fun slice(fromInclusive: K, toInclusive: K): Iterable<T> {
    return slice(fromInclusive, true, toInclusive, true)
  }

  fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T>
}

interface ResourceSet<K, T : Any> : BasicResourceSet<T>, UniqueKeyResourceSet<K, T> {
  override fun firstOrThrow(): T {
    return super.firstOrThrow()
  }

  override fun lastOrThrow(): T {
    return super.lastOrThrow()
  }

  override fun first(): Optional<T> {
    return super.first()
  }

  override fun last(): Optional<T> {
    return super.last()
  }
}

interface ExtensionResourceSet<E : ResourceExtension<T>, T : AbstractResource> {
  fun extension(): E
}

interface GroupedResourceSet<G, T : Any> {
  fun getGroup(groupId: G): Optional<BasicResourceSet<T>>

  @Nullable
  fun getGroupOrNull(groupId: G): BasicResourceSet<T>?

  fun getGroupOrThrow(groupId: G): BasicResourceSet<T>

  fun groupMap(): Map<G, BasicResourceSet<T>>

  fun containsGroup(groupId: G): Boolean
}
