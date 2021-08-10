package play.res

import java.util.*
import java.util.concurrent.ConcurrentHashMap

private typealias AnyDelegatedConfigSet = DelegatedResourceSet<Any, AbstractResource, Any, ResourceExtension<AbstractResource>>

open class DelegatedResourceSet<K, T, G, E> internal constructor() :
  SuperResourceSet<K, T, G, E> where T : AbstractResource, E : ResourceExtension<T> {

  companion object {
    private val instances = ConcurrentHashMap<Class<*>, AnyDelegatedConfigSet>()
    fun get(clazz: Class<*>) =
      instances[clazz] ?: throw IllegalStateException("ConfigSet for [${clazz.simpleName}] not initialized.")

    internal fun getOrNull(clazz: Class<*>): AnyResourceSet? {
      return instances[clazz]
    }

    internal fun createOrUpdate(clazz: Class<*>, configSet: AnyResourceSet) {
      instances.computeIfAbsent(clazz) {
        if (ResourceHelper.isSingletonResource(it)) {
          SingletonDelegatedResourceSet()
        } else {
          AnyDelegatedConfigSet()
        }
      }.updateDelegatee(configSet)
    }
  }

  private lateinit var delegatee: SuperResourceSet<K, T, G, E>

  internal fun delegatee() = delegatee

  internal fun updateDelegatee(delegatee: SuperResourceSet<K, T, G, E>) {
    this.delegatee = delegatee
  }

  override fun indexOf(elem: T): Int = delegatee.indexOf(elem)

  override fun getByIndexOrNull(idx: Int): T? = delegatee.getByIndexOrNull(idx)

  override fun contains(id: Int): Boolean = delegatee.contains(id)

  override fun get(id: Int): Optional<T> = delegatee.get(id)

  override fun getOrThrow(id: Int): T = delegatee.getOrThrow(id)

  override fun getOrNull(id: Int): T? = delegatee.getOrNull(id)

  override fun list(): List<T> = delegatee.list()

  override fun containsKey(key: K): Boolean = delegatee.containsKey(key)

  override fun getByKey(key: K): Optional<T> = delegatee.getByKey(key)

  override fun getByKeyOrNull(key: K): T? = delegatee.getByKeyOrNull(key)

  override fun getByKeyOrThrow(key: K): T = delegatee.getByKeyOrThrow(key)

  override fun nextOrThrow(key: K): T = delegatee.nextOrThrow(key)

  override fun next(key: K): Optional<T> = delegatee.next(key)

  override fun prevOrThrow(key: K): T = delegatee.nextOrThrow(key)

  override fun prev(key: K): Optional<T> = delegatee.prev(key)

  override fun prevOrEqualsOption(key: K): Optional<T> = delegatee.prevOrEqualsOption(key)

  override fun nextOrEqualsOption(key: K): Optional<T> = delegatee.nextOrEqualsOption(key)

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    return delegatee.slice(from, fromIncluded, to, toIncluded)
  }

  override fun extension(): E {
    return delegatee.extension()
  }

  override fun getGroup(groupId: G): Optional<BasicResourceSet<T>> = delegatee.getGroup(groupId)

  override fun getGroupOrNull(groupId: G): BasicResourceSet<T>? = delegatee.getGroupOrNull(groupId)

  override fun getGroupOrThrow(groupId: G): BasicResourceSet<T> = delegatee.getGroupOrThrow(groupId)

  override fun groupMap(): Map<G, BasicResourceSet<T>> = delegatee.groupMap()

  override fun containsGroup(groupId: G): Boolean = delegatee.containsGroup(groupId)
}

internal class SingletonDelegatedResourceSet<T : AbstractResource> :
  DelegatedResourceSet<Any, T, Any, ResourceExtension<T>>(), SingletonResourceSet<T> {
  @Suppress("UNCHECKED_CAST")
  override fun get(): T = (delegatee() as SingletonResourceSet<T>).get()
}
