package play.config

import kotlin.reflect.KClass

sealed class ConfigRefreshEvent constructor(
  val configClasses: Set<Class<out AbstractConfig>>,
) {
  fun <T : AbstractConfig> contains(clazz: Class<T>): Boolean {
    return configClasses.contains(clazz)
  }

  fun <T : AbstractConfig> contains(kclass: KClass<T>): Boolean {
    return configClasses.contains(kclass.java)
  }

  abstract fun isLoad(): Boolean

  abstract fun isReload(): Boolean
}

class ConfigLoadEvent internal constructor(
  configClasses: Set<Class<out AbstractConfig>>
) : ConfigRefreshEvent(configClasses) {
  override fun isLoad(): Boolean = true

  override fun isReload(): Boolean = false
}

class ConfigReloadEvent internal constructor(
  configClasses: Set<Class<out AbstractConfig>>
) : ConfigRefreshEvent(configClasses) {
  override fun isLoad(): Boolean = false

  override fun isReload(): Boolean = true
}
