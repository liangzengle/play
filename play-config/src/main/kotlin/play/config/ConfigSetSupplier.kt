package play.config

@Suppress("UNCHECKED_CAST")
class ConfigSetSupplier internal constructor(private val configSets: Map<Class<AbstractConfig>, AnyConfigSet>) {

  internal var dependentConfigs: MutableSet<Class<*>>? = null

  fun <T : AbstractConfig> get(clazz: Class<T>): BasicConfigSet<T> {
    dependentConfigs?.add(clazz)
    val configSet = configSets[clazz as Class<AbstractConfig>] ?: throw NoSuchElementException(clazz.name)
    return configSet as BasicConfigSet<T>
  }

  fun <T : AbstractConfig> getSingleton(clazz: Class<T>): SingletonConfigSet<T> {
    return get(clazz) as SingletonConfigSet<T>
  }

  inline fun <reified T : AbstractConfig> get(): BasicConfigSet<T> {
    return get(T::class.java)
  }
}
