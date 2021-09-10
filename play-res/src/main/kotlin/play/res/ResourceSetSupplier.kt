package play.res

@Suppress("UNCHECKED_CAST")
class ResourceSetSupplier internal constructor(private val resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>) {

  internal var dependentResources: MutableSet<Class<*>>? = null

  fun <T : AbstractResource> get(clazz: Class<T>): ResourceSet<T> {
    dependentResources?.add(clazz)
    val resourceSet = resourceSets[clazz as Class<AbstractResource>] ?: throw NoSuchElementException(clazz.name)
    return resourceSet as ResourceSet<T>
  }

  fun <T : AbstractResource> getSingleton(clazz: Class<T>): SingletonResourceSet<T> {
    return get(clazz) as SingletonResourceSet<T>
  }

  inline fun <reified T : AbstractResource> get(): ResourceSet<T> {
    return get(T::class.java)
  }
}
