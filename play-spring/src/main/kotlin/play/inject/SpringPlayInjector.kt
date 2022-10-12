package play.inject

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import play.util.logging.getLogger
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture

/**
 *
 * @author LiangZengle
 */
class SpringPlayInjector(private val ctx: ApplicationContext) : PlayInjector, SmartInitializingSingleton {
  private val logger = getLogger()

  private var instantiated = false

  private val future = CompletableFuture<ApplicationContext>()

  override fun <T> getInstance(type: Class<T>): T {
    if (!instantiated) throw IllegalStateException("Singletons NOT Instantiated: getInstance(${type.name})")
    return ctx.getBean(type)
  }

  override fun <T> getInstance(type: Type): T {
    if (!instantiated) throw IllegalStateException("Singletons NOT Instantiated: getInstance(${type})")
    val resolvableType = ResolvableType.forType(type)
    return ctx.getBeanProvider<T>(resolvableType).ifAvailable ?: throw NoSuchBeanDefinitionException(resolvableType)
  }

  override fun <T> getInstance(type: Class<T>, name: String): T {
    if (!instantiated) throw IllegalStateException("Singletons NOT Instantiated: getInstance(${type.name}, $name)")
    return ctx.getBean(name, type)
  }

  override fun <T> getInstancesOfType(type: Class<T>): Collection<T> {
    if (!instantiated) throw IllegalStateException("Singletons NOT Instantiated: getInstancesOfType(${type.name})")
    return ctx.getBeansOfType(type).values
  }

  override fun getInstancesAnnotatedWith(type: Class<out Annotation>): Collection<Any> {
    if (!instantiated) throw IllegalStateException("Singletons NOT Instantiated: getInstancesAnnotatedWith(${type.name})")
    return ctx.getBeansWithAnnotation(type).values
  }

  override fun afterSingletonsInstantiated() {
    instantiated = true
    future.complete(ctx)
  }

  fun whenAvailable(op: (ApplicationContext) -> Unit) {
    future.whenComplete { v, e ->
      assert(e == null)
      try {
        op(v)
      } catch (e: Exception) {
        logger.error(e.message, e)
      }
    }
  }
}
