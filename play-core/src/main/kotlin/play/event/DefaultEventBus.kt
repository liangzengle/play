package play.event

import com.google.common.collect.Maps
import mu.KLogging
import play.util.exception.isFatal
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class DefaultEventBus : EventBus {
  companion object : KLogging()

  private val subscribers = Maps.newConcurrentMap<Class<*>, CopyOnWriteArrayList<(Any) -> Unit>>()

  override fun publish(event: Any) {
    val subscribers = EventBusHelper.getSubscribers(event.javaClass, subscribers::get)
    if (!subscribers.hasNext()) {
      logger.debug { "No listener for event: $event" }
      return
    }
    for (subscriber in subscribers) {
      try {
        subscriber(event)
      } catch (e: Exception) {
        logger.error(e) { "Exception occurred when handling event: $event" }
        if (e.isFatal()) {
          throw e
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> subscribe(eventType: Class<T>, action: (T) -> Unit) {
    subscribers.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
      .add(action as (Any) -> Unit)
  }
}
