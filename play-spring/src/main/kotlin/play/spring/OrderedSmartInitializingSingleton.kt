package play.spring

import org.springframework.beans.factory.BeanFactory

/**
 *
 * @author LiangZengle
 */
interface OrderedSmartInitializingSingleton {

  fun afterSingletonsInstantiated(beanFactory: BeanFactory)
}
