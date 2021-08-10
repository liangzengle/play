package play.res

import play.util.reflect.Reflect

/**
 * 配置刷新监听器
 * Created by LiangZengle on 2020/2/23.
 */
fun interface ResourceReloadListener {
  fun onConfigReloaded(reloadedResources: Set<Class<out AbstractResource>>)
}

abstract class GenericResourceReloadListener<T : AbstractResource> : ResourceReloadListener {

  private val configClass: Class<T> =
    Reflect.getRawClass(Reflect.getTypeArg(javaClass, GenericResourceReloadListener::class.java, 0))

  override fun onConfigReloaded(reloadedResources: Set<Class<out AbstractResource>>) {
    if (reloadedResources.contains(configClass)) {
      onReload()
    }
  }

  protected abstract fun onReload()
}

abstract class SubtypeResourceReloadListener<SuperType> : ResourceReloadListener {
  private val superType: Class<SuperType> =
    Reflect.getRawClass(Reflect.getTypeArg(javaClass, SubtypeResourceReloadListener::class.java, 0))

  override fun onConfigReloaded(reloadedResources: Set<Class<out AbstractResource>>) {
    if (reloadedResources.any { superType.isAssignableFrom(it) }) {
      onReload()
    }
  }

  protected abstract fun onReload()
}
