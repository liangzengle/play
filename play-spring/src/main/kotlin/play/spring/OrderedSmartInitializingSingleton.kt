package play.spring

import org.springframework.beans.factory.BeanFactory

/**
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 * @see play.Order
 * @author LiangZengle
 */
interface OrderedSmartInitializingSingleton {

  fun afterSingletonsInstantiated(beanFactory: BeanFactory)
}
