package play.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(SpringEventBusConfiguration::class)
annotation class EnableSpringEventBus

@Configuration(proxyBeanMethods = false)
class SpringEventBusConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun eventBus(applicationContext: ApplicationContext): EventBus {
    return SpringEventBus(applicationContext)
  }
}
