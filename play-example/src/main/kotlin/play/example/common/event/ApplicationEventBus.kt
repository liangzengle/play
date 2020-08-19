package play.example.common.event

import play.Log
import play.util.reflect.isAbstract
import play.util.unsafeCast
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Singleton

/**
 * 系统事件总线
 * @author LiangZengle
 */
@Singleton
class ApplicationEventBus {

  private val classifiedSubscribers: ConcurrentMap<Class<*>, MutableList<(ApplicationEvent) -> Unit>> =
    ConcurrentHashMap()

  fun postBlocking(event: ApplicationEvent) {
    val subscribers = classifiedSubscribers[event.javaClass]
    if (subscribers == null) {
      Log.warn { "No subscriber for event: $event" }
      return
    }
    for (i in subscribers.indices) {
      val subscriber = subscribers[i]
      try {
        subscriber(event)
      } catch (e: Exception) {
        Log.error(e) { "事件处理失败: $event" }
      }
    }
  }

  fun <T : ApplicationEvent> subscribe(eventType: Class<T>, action: (T) -> Unit) {
    if (eventType.isAbstract()) {
      throw UnsupportedOperationException("Subscribe subclass is not supported.")
    }
    classifiedSubscribers.compute(eventType) { _, v ->
      val subscribers = v ?: ArrayList(4) // sine removal is impossible, ArrayList is just fine.
      subscribers.add(action.unsafeCast())
      subscribers
    }
  }
}
