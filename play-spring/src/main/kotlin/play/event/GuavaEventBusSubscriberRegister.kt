package play.event

import mu.KLogging
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import java.util.*

class GuavaEventBusSubscriberRegister(private val eventBus: GuavaEventBus) : SmartInitializingSingleton,
  ApplicationContextAware {
  companion object : KLogging()

  private lateinit var applicationContext: ApplicationContext

  @Suppress("UnstableApiUsage")
  override fun afterSingletonsInstantiated() {
    val guavaEventBus = eventBus.asGuava()
    // TODO avoid scan all beans
    Arrays.stream(applicationContext.beanDefinitionNames)
      .parallel()
      .forEach {
        val bean = applicationContext.getBean(it)
        guavaEventBus.register(bean)
      }
  }

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.applicationContext = applicationContext
  }
}
