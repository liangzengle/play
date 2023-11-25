package play.util

import play.util.reflect.Reflect
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.NoSuchElementException

/**
 *
 * @author LiangZengle
 */
object ServiceLoader2 {

  private val implementationMap = ConcurrentHashMap<Class<*>, Any>()

  /**
   * 将[serviceClass]的实现指定为[implClass]，将无视META-INF/services中的配置
   * @param serviceClass service接口类
   * @param implClass service实现类
   */
  @JvmStatic
  fun <T : Any> setImpl(serviceClass: Class<T>, implClass: Class<out T>) {
    setImpl(serviceClass, Reflect.newInstance(implClass))
  }

  /**
   * 将[serviceClass]的实现指定为[impl]，将无视META-INF/services中的配置
   * @param serviceClass service接口类
   * @param impl service实例
   */
  @JvmStatic
  fun <T : Any> setImpl(serviceClass: Class<T>, impl: T) {
    implementationMap[serviceClass] = impl
  }

  /**
   * 加载META-INF/services配置的实现，如果不存在则抛出异常[NoSuchElementException]
   * @param serviceClass service接口类
   * @return service实例
   */
  @JvmStatic
  fun <T> load(serviceClass: Class<T>): T {
    return loadOrNull(serviceClass)
      ?: throw NoSuchElementException("Could not find any implementations of ${serviceClass.name}")
  }

  /**
   * 加载META-INF/services配置的实现，如果不存在则返回[default]
   * @param serviceClass service接口类
   * @param default 默认实现
   * @return service实例
   */
  @JvmStatic
  fun <T> loadOrDefault(serviceClass: Class<T>, default: () -> T): T {
    return loadOrNull(serviceClass) ?: default()
  }

  /**
   * 加载META-INF/services配置的实现，如果不存在则返回null
   * @param serviceClass service接口类
   * @return service实例或者null
   */
  @JvmStatic
  fun <T> loadOrNull(serviceClass: Class<T>): T? {
    val superior = implementationMap[serviceClass]
    if (superior != null) {
      @Suppress("UNCHECKED_CAST")
      return superior as T
    }
    return ServiceLoader.load(serviceClass).findFirst().getOrNull()
  }
}
