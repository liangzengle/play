@file:JvmName("Spring")

package play.spring

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

fun KType.toResolvableType() = ResolvableType.forType(this.javaType)

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

  
