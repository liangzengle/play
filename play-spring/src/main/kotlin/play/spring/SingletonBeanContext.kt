package play.spring

import org.jctools.maps.NonBlockingHashMap
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import play.util.LambdaClassValue
import play.util.collection.toImmutableMap
import play.util.getOrNull
import play.util.unsafeCast
import java.util.*

class SingletonBeanContext(val spring: ApplicationContext) {

  private val singletonBeanCache = NonBlockingHashMap<Any, Optional<Any>>()

  private val beanWithTypeCache = LambdaClassValue { type ->
    spring.getBeansOfType(type).values.stream()
      .toImmutableMap { it.unsafeCast<BeanWithType<Any>>().type() }
  }

  fun <T : Any> getBean(beanType: Class<T>): T {
    return getBeanOrNull(beanType) ?: throw NoSuchBeanDefinitionException(beanType)
  }

  fun <T : Any> getBean(beanType: ResolvableType): T {
    return getBeanOrNull(beanType) ?: throw NoSuchBeanDefinitionException(beanType)
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getBeanOrNull(beanType: Class<T>): T? {
    val maybeBean = singletonBeanCache[beanType]
    if (maybeBean != null) {
      return maybeBean.getOrNull() as? T
    }
    val bean = spring.getBeanProvider(beanType).ifUnique
    singletonBeanCache[beanType] = Optional.ofNullable(bean)
    return bean
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getBeanOrNull(beanType: ResolvableType): T? {
    val maybeBean = singletonBeanCache[beanType]
    if (maybeBean != null) {
      return maybeBean.getOrNull() as? T
    }
    val bean = spring.getBeanProvider<T>(beanType).ifUnique
    singletonBeanCache[beanType] = Optional.ofNullable(bean)
    return bean
  }

  @Suppress("UNCHECKED_CAST")
  fun <T, R : BeanWithType<T>> getImplementation(superType: Class<out R>, typeValue: T): R {
    return beanWithTypeCache.get(superType)[typeValue as Any] as R
  }

  @Suppress("UNCHECKED_CAST")
  fun <T, R : BeanWithType<T>> getImplementations(superType: Class<out R>): Map<T, R> {
    return beanWithTypeCache.get(superType) as Map<T, R>
  }

  override fun equals(other: Any?): Boolean {
    return other is SingletonBeanContext && other.spring == this.spring
  }

  override fun hashCode(): Int {
    return spring.hashCode()
  }

  override fun toString(): String {
    return spring.toString()
  }
}
