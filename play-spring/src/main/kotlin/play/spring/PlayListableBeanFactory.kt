package play.spring

import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import play.Orders
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
    val beanNames = arrayListOf(*beanDefinitionNames)
    val beans = arrayListOf<OrderedSmartInitializingSingleton>()
    for (beanName in beanNames) {
      val bean = getSingleton(beanName)
      if (bean is OrderedSmartInitializingSingleton) {
        beans.add(bean)
      }
    }
    beans.sortedBy(::getOrder)
    for (bean in beans) {
      bean.afterSingletonsInstantiated(this)
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
}
