package play.event

import org.springframework.context.ApplicationContext
import play.spring.OrderedSmartInitializingSingleton

/**
 *
 *
 * @author LiangZengle
 */
class EventListenerAutoRegister(private val eventBus: EventBus, private val applicationContext: ApplicationContext) :
  OrderedSmartInitializingSingleton {
  override fun afterSingletonsInstantiated() {
    applicationContext.getBeanProvider(EventListener::class.java).forEach(eventBus::register)
  }
}
