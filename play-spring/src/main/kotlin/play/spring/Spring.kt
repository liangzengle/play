@file:JvmName("Spring")

package play.spring

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.ResolvableType
import play.Log
import play.util.unsafeCast
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

fun KType.toResolvableType() = ResolvableType.forType(this.javaType)

fun ResolvableType.getRawClassNotNull(): Class<*> = checkNotNull(rawClass) { "rawClass is null: $this" }

fun <T : Any> BeanDefinitionRegistry.registerBeanDefinition(beanName: String, kType: KType, instance: T) {
  registerBeanDefinition(beanName, beanDefinition(kType.toResolvableType(), instance))
}

fun <T : Any> beanDefinition(resolvableType: ResolvableType, instance: T): RootBeanDefinition {
  val rawClass = resolvableType.getRawClassNotNull()
  require(rawClass.isAssignableFrom(instance.javaClass))
  return RootBeanDefinition(rawClass.unsafeCast()) { instance }
}

fun <T : Any> beanDefinition(kType: KType, instance: T): RootBeanDefinition {
  val resolvableType = kType.toResolvableType()
  val rawClass = resolvableType.getRawClassNotNull()
  require(rawClass.isAssignableFrom(instance.javaClass)) { "type doesn't match: $rawClass vs ${instance.javaClass}" }
  val definition = RootBeanDefinition(rawClass.unsafeCast()) { instance }
  definition.setTargetType(resolvableType)
  return definition
}

fun beanDefinition(kType: KType): BeanDefinition {
  val resolvableType = kType.toResolvableType()
  val rawClass = resolvableType.getRawClassNotNull()
  val definition = RootBeanDefinition(rawClass)
  definition.setTargetType(resolvableType)
  return definition
}

inline fun <reified T> ListableBeanFactory.getInstance(): T {
  val type = typeOf<T>()
  return getBeanProvider<T>(type.toResolvableType()).ifUnique
    ?: throw IllegalStateException("No unique bean for type: $type")
}

inline fun <reified T> ListableBeanFactory.getInstances(): Collection<T> {
  return getBeansOfType(T::class.java).values
}

inline fun <reified T> BeanFactory.getInstance(name: String): T {
  return getBean(name, T::class.java)
}

private const val SLEEP = 50

private val TIMEOUT = TimeUnit.MINUTES.toMillis(10)

fun ConfigurableApplicationContext.closeAndWait() {
  if (!isActive) {
    return
  }
  close()
  try {
    var waited = 0
    while (isActive) {
      if (waited > TIMEOUT) {
        throw TimeoutException()
      }
      Thread.sleep(SLEEP.toLong())
      waited += SLEEP
    }
  } catch (ex: InterruptedException) {
    Thread.currentThread().interrupt()
    Log.warn("Interrupted waiting for application context $this to become inactive")
  } catch (ex: TimeoutException) {
    Log.warn(
      "Timed out waiting for application context $this to become inactive",
      ex
    )
  }
}

  
