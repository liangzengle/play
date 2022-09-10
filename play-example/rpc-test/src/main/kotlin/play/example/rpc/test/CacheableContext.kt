package play.example.rpc.test

import org.springframework.context.ApplicationContext
import org.springframework.core.ResolvableType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class CacheableContext(private val ctx: ApplicationContext) {

  private val cache = ConcurrentHashMap<Any, Optional<Any>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getSingleton(beanType: Class<T>): T? {
    val maybeBean = cache[beanType]
    if (maybeBean != null) {
      return maybeBean.orElse(null) as? T
    }
    val bean = ctx.getBeanProvider(beanType).ifUnique
    cache[beanType] = Optional.ofNullable(bean)
    return bean
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getSingleton(beanType: ResolvableType): T? {
    val maybeBean = cache[beanType]
    if (maybeBean != null) {
      return maybeBean.orElse(null) as? T
    }
    val bean = ctx.getBeanProvider<T>(beanType).ifUnique
    cache[beanType] = Optional.ofNullable(bean)
    return bean
  }
}
