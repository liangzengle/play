package play.config

/**
 * 配置事件监听器，
 * Created by LiangZengle on 2020/2/23.
 */
interface ConfigEventListener {
  fun afterLoaded()

  fun afterReloaded(reloadedClasses: Set<Class<*>>)
}
