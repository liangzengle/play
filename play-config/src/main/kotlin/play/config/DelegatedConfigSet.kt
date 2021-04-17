package play.config

import java.util.*
import java.util.concurrent.ConcurrentHashMap

private typealias AnyDelegatedConfigSet = DelegatedConfigSet<Any, AbstractConfig, Any, ConfigExtension<AbstractConfig>>

open class DelegatedConfigSet<K, T, G, E> internal constructor() :
  SuperConfigSet<K, T, G, E> where T : AbstractConfig, E : ConfigExtension<T> {

  companion object {
    private val instances = ConcurrentHashMap<Class<*>, AnyDelegatedConfigSet>()
    fun get(clazz: Class<*>) = instances.computeIfAbsent(clazz) {
      if (it.isAnnotationPresent(SingletonConfig::class.java) ||
        it.isAnnotationPresent(Resource::class.java)
      ) {
        SingletonDelegatedConfigSet()
      } else {
        AnyDelegatedConfigSet()
      }
    }
  }

  private lateinit var delegatee: SuperConfigSet<K, T, G, E>

  internal fun delegatee() = delegatee

  internal fun updateDelegatee(delegatee: SuperConfigSet<K, T, G, E>) {
    this.delegatee = delegatee
  }

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

  override fun equalsOrPrevOption(key: K): Optional<T> = delegatee.equalsOrPrevOption(key)

  override fun equalsOrNextOption(key: K): Optional<T> = delegatee.equalsOrNextOption(key)

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    return delegatee.slice(from, fromIncluded, to, toIncluded)
  }

  override fun extension(): E {
    return delegatee.extension()
  }

  override fun getGroup(groupId: G): Optional<BasicConfigSet<T>> = delegatee.getGroup(groupId)

  override fun getGroupOrNull(groupId: G): BasicConfigSet<T>? = delegatee.getGroupOrNull(groupId)

  override fun getGroupOrThrow(groupId: G): BasicConfigSet<T> = delegatee.getGroupOrThrow(groupId)

  override fun groupMap(): Map<G, BasicConfigSet<T>> = delegatee.groupMap()

  override fun containsGroup(groupId: G): Boolean = delegatee.containsGroup(groupId)
}

internal class SingletonDelegatedConfigSet<T : AbstractConfig> :
  DelegatedConfigSet<Any, T, Any, ConfigExtension<T>>(), SingletonConfigSet<T> {
  @Suppress("UNCHECKED_CAST")
  override fun get(): T = (delegatee() as SingletonConfigSet<T>).get()
}
