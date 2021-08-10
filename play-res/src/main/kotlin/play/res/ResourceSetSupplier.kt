package play.res

@Suppress("UNCHECKED_CAST")
class ResourceSetSupplier internal constructor(private val configSets: Map<Class<AbstractResource>, AnyResourceSet>) {

  internal var dependentConfigs: MutableSet<Class<*>>? = null

  fun <T : AbstractResource> get(clazz: Class<T>): BasicResourceSet<T> {
    dependentConfigs?.add(clazz)
    val configSet = configSets[clazz as Class<AbstractResource>] ?: throw NoSuchElementException(clazz.name)
    return configSet as BasicResourceSet<T>
  }

  fun <T : AbstractResource> getSingleton(clazz: Class<T>): SingletonResourceSet<T> {
    return get(clazz) as SingletonResourceSet<T>
  }

  inline fun <reified T : AbstractResource> get(): BasicResourceSet<T> {
    return get(T::class.java)
  }
}
