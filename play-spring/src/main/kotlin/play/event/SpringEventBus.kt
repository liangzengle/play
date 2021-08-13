package play.event

import org.springframework.context.ApplicationContext
import java.util.concurrent.Executor

class SpringEventBus(private val applicationContext: ApplicationContext) : EventBus {
  override fun postSync(event: Any) {
    applicationContext.publishEvent(event)
  }

  override fun postAsync(event: Any, executor: Executor) {
    executor.execute { postSync(event) }
  }
}
