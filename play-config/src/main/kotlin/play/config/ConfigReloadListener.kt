package play.config

import play.util.reflect.Reflect

/**
 * 配置刷新监听器
 * Created by LiangZengle on 2020/2/23.
 */
interface ConfigReloadListener {
  fun onConfigReloaded(reloadedConfigClasses: Set<Class<out AbstractConfig>>)
}

abstract class AbstractConfigReloadListener(private val interested: Set<Class<out AbstractConfig>>) :
  ConfigReloadListener {
  constructor(configClass: Class<out AbstractConfig>) : this(setOf(configClass))

  final override fun onConfigReloaded(reloadedConfigClasses: Set<Class<out AbstractConfig>>) {
    if (interested.any(reloadedConfigClasses::contains)) {
      onReload()
    }
  }

  protected abstract fun onReload()
}

abstract class GenericConfigReloadListener<T : AbstractConfig> : ConfigReloadListener {

  private val configClass: Class<T> =
    Reflect.getRawClass(Reflect.getTypeArg(javaClass, GenericConfigReloadListener::class.java, 0))

  override fun onConfigReloaded(reloadedConfigClasses: Set<Class<out AbstractConfig>>) {
    if (reloadedConfigClasses.contains(configClass)) {
      onReload()
    }
  }

  protected abstract fun onReload()
}
