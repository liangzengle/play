package play.config

import io.vavr.control.Option
import java.util.concurrent.ConcurrentHashMap

class DelegatedConfigSet<K, T, G, E> private constructor() :
  SuperConfigSet<K, T, G, E> where T : AbstractConfig, E : ConfigExtension<T> {

  companion object {
    private val instances = ConcurrentHashMap<Class<*>, AnyDelegatedConfigSet>()
    fun get(clazz: Class<*>) = instances.computeIfAbsent(clazz) { AnyDelegatedConfigSet() }
  }

  private lateinit var delegatee: SuperConfigSet<K, T, G, E>

  internal fun delegatee() = delegatee

  internal fun updateDelegatee(delegatee: SuperConfigSet<K, T, G, E>) {
    this.delegatee = delegatee
  }

  override fun contains(id: Int): Boolean = delegatee.contains(id)

  override fun get(id: Int): Option<T> = delegatee.get(id)

  override fun getOrThrow(id: Int): T = delegatee.getOrThrow(id)

  override fun getOrNull(id: Int): T? = delegatee.getOrNull(id)

  override fun list(): List<T> = delegatee.list()

  override fun containsKey(key: K): Boolean = delegatee.containsKey(key)

  override fun getByKey(key: K): Option<T> = delegatee.getByKey(key)

  override fun getByKeyOrNull(key: K): T? = delegatee.getByKeyOrNull(key)

  override fun getByKeyOrThrow(key: K): T = delegatee.getByKeyOrThrow(key)

  override fun nextOrThrow(key: K): T = delegatee.nextOrThrow(key)

  override fun next(key: K): Option<T> = delegatee.next(key)

  override fun prevOrThrow(key: K): T = delegatee.nextOrThrow(key)

  override fun prev(key: K): Option<T> = delegatee.prev(key)

  override fun equalsOrPrevOption(key: K): Option<T> = delegatee.equalsOrPrevOption(key)

  override fun equalsOrNextOption(key: K): Option<T> = delegatee.equalsOrNextOption(key)

  override fun slice(from: K, fromIncluded: Boolean, to: K, toIncluded: Boolean): Iterable<T> {
    return delegatee.slice(from, fromIncluded, to, toIncluded)
  }

  override fun extension(): E {
    return delegatee.extension()
  }

  override fun getGroup(groupId: G): Option<ConfigSet<K, T>> = delegatee.getGroup(groupId)

  override fun getGroupOrNull(groupId: G): ConfigSet<K, T>? = delegatee.getGroupOrNull(groupId)

  override fun getGroupOrThrow(groupId: G): ConfigSet<K, T> = delegatee.getGroupOrThrow(groupId)

  override fun groupMap(): Map<G, ConfigSet<K, T>> = delegatee.groupMap()

  override fun containsGroup(groupId: G): Boolean = delegatee.containsGroup(groupId)
}
