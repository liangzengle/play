package play.spring

import jakarta.annotation.Priority
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import play.Orders
import play.util.Sorting
import play.util.time.Time
import java.time.Duration
import java.util.*

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
    val initializingBeanMap = LinkedHashMap<String, OrderedBean>()
    var asyncInitializing: AsyncInitializingSupport? = null
    for (beanName in beanDefinitionNames) {
      val bean = getSingleton(beanName)
      if (bean is OrderedSmartInitializingSingleton) {
        val dependencies = getDependenciesForBean(beanName)
        initializingBeanMap[beanName] = OrderedBean(beanName, dependencies, getOrder(bean), bean)
      } else if (bean is AsyncInitializingSupport) {
        asyncInitializing = bean
      }
    }
    val sorted = Sorting.topologicalSort(initializingBeanMap.values) { bean ->
      bean.dependencies.mapNotNull(initializingBeanMap::get)
    }
    for (bean in sorted) {
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

  private class OrderedBean(
    val beanName: String,
    val dependencies: Array<String>,
    val order: Int,
    val bean: OrderedSmartInitializingSingleton
  ) : Comparable<OrderedBean>, OrderedSmartInitializingSingleton by bean {
    override fun compareTo(other: OrderedBean): Int {
      if (this.dependencies.contains(other.beanName)) {
        return 1
      }
      if (other.dependencies.contains(this.beanName)) {
        return -1
      }
      var r = this.order.compareTo(other.order)
      if (r == 0) {
        r = this.beanName.compareTo(other.beanName)
      }
      return r
    }

    override fun toString(): String {
      return beanName
    }
  }
}
