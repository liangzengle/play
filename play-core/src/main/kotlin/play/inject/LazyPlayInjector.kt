package play.inject

import java.lang.reflect.Type

/**
 *
 * @author LiangZengle
 */
class LazyPlayInjector(private val lazyInjector: Lazy<PlayInjector>) : PlayInjector {
  override fun <T> getInstance(type: Class<T>): T {
    return lazyInjector.value.getInstance(type)
  }

  override fun <T> getInstance(type: Type): T {
    return lazyInjector.value.getInstance(type)
  }

  override fun <T> getInstance(type: Class<T>, name: String): T {
    return lazyInjector.value.getInstance(type, name)
  }

  override fun <T> getInstancesOfType(type: Class<T>): Collection<T> {
    return lazyInjector.value.getInstancesOfType(type)
  }

  override fun getInstancesAnnotatedWith(type: Class<out Annotation>): Collection<Any> {
    return lazyInjector.value.getInstancesAnnotatedWith(type)
  }

}
