package play.res

import play.Log
import play.util.unsafeCastOrNull

open class DelegatingResourceSet<T : AbstractResource> internal constructor(private var delegatee: ResourceSet<T>) :
  ResourceSet<T> {

  companion object {
    private var instances = emptyMap<Class<*>, DelegatingResourceSet<AbstractResource>>()

    fun <T : AbstractResource> getOrThrow(clazz: Class<*>): DelegatingResourceSet<T> {
      return getOrNull(clazz)
        ?: throw IllegalStateException("DelegatingResourceSet for [${clazz.simpleName}] is not initialized.")
    }

    fun <T : AbstractResource> getOrNull(clazz: Class<*>): DelegatingResourceSet<T>? {
      return instances[clazz].unsafeCastOrNull<DelegatingResourceSet<T>>()
    }

    internal fun update(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>) {
      for ((k, v) in resourceSets.entries) {
        val delegated = instances[k]
        if (delegated != null) {
          delegated.updateDelegatee(v)
        } else {
          Log.error { "DelegatingResourceSet not exists, update failed: ${k.simpleName}" }
        }
      }
    }

    internal fun init(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>) {
      instances = resourceSets.mapValues {
        check(it !is DelegatingResourceSet<*>)
        DelegatingResourceSet(it.value)
      }
    }
  }

  internal fun updateDelegatee(delegatee: ResourceSet<T>) {
    this.delegatee = delegatee
  }

  fun getDelegatee() = delegatee

  override fun indexOf(elem: T): Int = delegatee.indexOf(elem)

  override fun getByIndexOrNull(idx: Int): T? = delegatee.getByIndexOrNull(idx)

  override fun contains(id: Int): Boolean = delegatee.contains(id)

  override fun getOrNull(id: Int): T? = delegatee.getOrNull(id)

  override fun list(): List<T> = delegatee.list()
}
