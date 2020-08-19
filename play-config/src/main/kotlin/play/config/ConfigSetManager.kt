package play.config

class ConfigSetManager internal constructor(private val configSets: Map<Class<AbstractConfig>, BasicConfigSet<AbstractConfig>>) {

  @Suppress("UNCHECKED_CAST")
  fun <T : AbstractConfig> get(clazz: Class<T>): BasicConfigSet<T> {
    return configSets[clazz as Class<AbstractConfig>] as BasicConfigSet<T>
  }

  inline fun <reified T : AbstractConfig> get(): BasicConfigSet<T> {
    return get(T::class.java)
  }
}
