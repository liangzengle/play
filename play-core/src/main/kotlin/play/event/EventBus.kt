package play.event

import java.util.concurrent.Executor

interface EventBus {

  fun postSync(event: Any)

  fun postAsync(event: Any, executor: Executor)
}

@Suppress("UnstableApiUsage")
class GuavaEventBus(name: String) : EventBus {
  private val guava = com.google.common.eventbus.EventBus(name)

  override fun postSync(event: Any) {
    guava.post(event)
  }

  override fun postAsync(event: Any, executor: Executor) {
    executor.execute { postSync(event) }
  }

  fun asGuava() = guava
}
