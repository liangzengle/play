package play.event

import org.springframework.beans.factory.BeanFactory
import play.spring.OrderedSmartInitializingSingleton

/**
 *
 *
 * @author LiangZengle
 */
class EventListenerAutoRegister(private val eventBus: EventBus) : OrderedSmartInitializingSingleton {
  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    beanFactory.getBeanProvider(EventListener::class.java).forEach(eventBus::register)
  }
}
