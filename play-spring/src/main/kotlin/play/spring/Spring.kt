@file:JvmName("Spring")

package play.spring

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.core.ResolvableType
import play.util.unsafeCast
import kotlin.reflect.KType
import kotlin.reflect.javaType

fun KType.toResolvableType() = ResolvableType.forType(this.javaType)

fun BeanDefinitionRegistry.registerBeanDefinition(beanName: String, beanDefinition: BeanDefinition) {
  registerBeanDefinition(beanName, beanDefinition)
}

fun <T : Any> BeanDefinitionRegistry.registerBeanDefinition(beanName: String, kType: KType, instance: T) {
  registerBeanDefinition(beanName, rootBeanDefinition(kType.toResolvableType(), instance))
}

fun <T : Any> rootBeanDefinition(resolvableType: ResolvableType, instance: T): RootBeanDefinition {
  require(resolvableType.rawClass.isAssignableFrom(instance.javaClass))
  return RootBeanDefinition(resolvableType.rawClass.unsafeCast()) { instance }
}

fun <T : Any> rootBeanDefinition(kType: KType, instance: T): RootBeanDefinition {
  val resolvableType = kType.toResolvableType()
  require(resolvableType.rawClass.isAssignableFrom(instance.javaClass)) { "type doesn't match: ${resolvableType.rawClass} vs ${instance.javaClass}" }
  val definition = RootBeanDefinition(resolvableType.rawClass.unsafeCast()) { instance }
  definition.setTargetType(resolvableType)
  return definition
}

  
