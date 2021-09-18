package play.res

import play.util.reflect.Reflect

/**
 * 配置刷新监听器
 * Created by LiangZengle on 2020/2/23.
 */
fun interface ResourceReloadListener {
  fun onResourceReloaded(reloadedResources: Set<Class<out AbstractResource>>)
}

abstract class GenericResourceReloadListener<T : AbstractResource> : ResourceReloadListener {

  private val resourceClass: Class<T> =
    Reflect.getRawClassOfTypeArg(javaClass, GenericResourceReloadListener::class.java, 0)

  override fun onResourceReloaded(reloadedResources: Set<Class<out AbstractResource>>) {
    if (reloadedResources.contains(resourceClass)) {
      onReload()
    }
  }

  protected abstract fun onReload()
}

abstract class SubtypeResourceReloadListener<SuperType> : ResourceReloadListener {
  private val superType: Class<SuperType> =
    Reflect.getRawClassOfTypeArg(javaClass, SubtypeResourceReloadListener::class.java, 0)

  override fun onResourceReloaded(reloadedResources: Set<Class<out AbstractResource>>) {
    if (reloadedResources.any { superType.isAssignableFrom(it) }) {
      onReload()
    }
  }

  protected abstract fun onReload()
}
