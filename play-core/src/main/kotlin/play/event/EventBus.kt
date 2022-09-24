package play.event

import com.google.common.collect.Maps
import com.google.common.reflect.TypeToken
import mu.KLogging
import play.util.LambdaClassValue
import play.util.collection.toImmutableList
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

class EventBus {
  companion object : KLogging()

  private val eventListeners = Maps.newConcurrentMap<Class<*>, CopyOnWriteArrayList<(Any) -> Unit>>()

  private val flattenHierarchyCache = LambdaClassValue { type ->
    val types = TypeToken.of(type).types.rawTypes().asSequence().filter { it !== Any::class.java }.toImmutableList()
    assert(types.isNotEmpty())
    if (types.size == 1) types[0] else types
  }

  private fun getEventListeners(eventType: Class<*>): Iterator<(Any) -> Unit> {
    val eventTypes = flattenHierarchyCache.get(eventType)
    if (eventTypes === eventType) {
      return eventListeners[eventType]?.iterator() ?: Collections.emptyIterator()
    }
    @Suppress("UNCHECKED_CAST")
    eventTypes as List<Class<*>>
    return eventTypes.asSequence().flatMap { eventListeners[it] ?: emptyList() }.iterator()
  }

  fun register(listener: EventListener) {
    val eventReceive = listener.eventReceive()
    for ((eventType, handler) in eventReceive.receive) {
      if (eventType == Any::class.java) {
        throw IllegalArgumentException("Can not listen ${eventType.name}")
      }
      eventListeners.computeIfAbsent(eventType) { CopyOnWriteArrayList() }.add(handler)
    }
  }

  fun post(event: Any) {
    val eventListeners = getEventListeners(event.javaClass)
    if (!eventListeners.hasNext()) {
      logger.debug { "No listener for event: $event" }
      return
    }
    for (eventListener in eventListeners) {
      try {
        eventListener(event)
      } catch (e: Exception) {
        logger.error(e) { "Exception occurred when handling event: $event" }
      }
    }
  }

  fun postAsync(event: Any, executor: Executor) {
    executor.execute { post(event) }
  }
}
