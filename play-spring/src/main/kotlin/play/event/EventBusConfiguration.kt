package play.event

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class EventBusConfiguration {

  @Bean
  fun eventBus(): EventBus {
    return EventBus()
  }

  @Bean
  fun eventListenerAutoRegister(eventBus: EventBus, applicationContext: ApplicationContext): EventListenerAutoRegister {
    return EventListenerAutoRegister(eventBus, applicationContext)
  }
}
