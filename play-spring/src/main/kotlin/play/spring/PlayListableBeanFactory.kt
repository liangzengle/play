package play.spring

import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import play.Orders
import play.util.time.Time
import java.time.Duration
import javax.annotation.Priority

/**
 *
 * @author LiangZengle
 */
class PlayListableBeanFactory() : DefaultListableBeanFactory() {
  constructor(parentBeanFactory: BeanFactory) : this() {
    setParentBeanFactory(parentBeanFactory)
  }

  @Throws(BeansException::class)
  override fun preInstantiateSingletons() {
    super.preInstantiateSingletons()
    val initializingSingletons = arrayListOf<OrderedBean>()
    var asyncInitializing: AsyncInitializingSupport? = null
    for (beanName in beanDefinitionNames) {
      val bean = getSingleton(beanName)
      if (bean is OrderedSmartInitializingSingleton) {
        initializingSingletons.add(OrderedBean(getOrder(bean), bean))
      } else if (bean is AsyncInitializingSupport) {
        asyncInitializing = bean
      }
    }
    getBeanProvider(OrderedSmartInitializingSingleton::class.java)
    initializingSingletons.sort()
    for (bean in initializingSingletons) {
      bean.afterSingletonsInstantiated()
    }
    if (asyncInitializing != null) {
      val timeout =
        System.getProperty(AsyncInitializingSupport.Timeout)?.let { Time.parseDuration(it) }
          ?: Duration.ofSeconds(60)
      asyncInitializing.await(timeout)
    }
  }

  private fun getOrder(bean: Any): Int {
    if (bean is Ordered) {
      return bean.order
    }
    val springOrder = bean.javaClass.getAnnotation(Order::class.java)
    if (springOrder != null) {
      return springOrder.value
    }
    val priority = bean.javaClass.getAnnotation(Priority::class.java)
    if (priority != null) {
      return priority.value
    }
    return Orders.getOrder(bean.javaClass)
  }

  private class OrderedBean(val order: Int, val bean: OrderedSmartInitializingSingleton) : Comparable<OrderedBean>,
    OrderedSmartInitializingSingleton by bean {
    override fun compareTo(other: OrderedBean): Int {
      return this.order.compareTo(other.order)
    }
  }
}
