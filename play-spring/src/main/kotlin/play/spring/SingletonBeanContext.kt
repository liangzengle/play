package play.spring

import org.jctools.maps.NonBlockingHashMap
import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import play.util.getOrNull
import java.util.*

class SingletonBeanContext(val context: ApplicationContext) {

  private val cache = NonBlockingHashMap<Any, Optional<Any>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getSingleton(beanType: Class<T>): T? {
    val maybeBean = cache[beanType]
    if (maybeBean != null) {
      return maybeBean.getOrNull() as? T
    }
    val bean = context.getBeanProvider(beanType).ifUnique
    cache[beanType] = Optional.ofNullable(bean)
    return bean
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getSingleton(beanType: ResolvableType): T? {
    val maybeBean = cache[beanType]
    if (maybeBean != null) {
      return maybeBean.getOrNull() as? T
    }
    val bean = context.getBeanProvider<T>(beanType).ifUnique
    cache[beanType] = Optional.ofNullable(bean)
    return bean
  }

  override fun equals(other: Any?): Boolean {
    return other is SingletonBeanContext && other.context == this.context
  }

  override fun hashCode(): Int {
    return context.hashCode()
  }

  override fun toString(): String {
    return context.toString()
  }
}
