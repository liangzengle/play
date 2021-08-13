package play.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(GuavaEventBusConfiguration::class)
annotation class EnableGuavaEventBus

@Configuration(proxyBeanMethods = false)
class GuavaEventBusConfiguration {

  @Bean
  @ConditionalOnMissingBean(EventBus::class)
  fun eventBus(): GuavaEventBus {
    return GuavaEventBus("default")
  }

  @Bean
  @ConditionalOnBean(GuavaEventBus::class)
  fun guavaEventBusSubscriberRegister(guavaEventBus: GuavaEventBus): GuavaEventBusSubscriberRegister {
    return GuavaEventBusSubscriberRegister(guavaEventBus)
  }
}
