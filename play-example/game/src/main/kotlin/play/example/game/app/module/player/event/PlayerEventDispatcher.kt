package play.example.game.app.module.player.event

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.reflect.TypeToken
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.inject.PlayInjector
import play.spring.OrderedSmartInitializingSingleton
import play.util.LambdaClassValue
import play.util.collection.toImmutableList
import play.util.exception.NoStackTraceException
import play.util.exception.isFatal
import play.util.logging.getLogger

@Component
class PlayerEventDispatcher(private val injector: PlayInjector) :
  OrderedSmartInitializingSingleton {
  private val logger = getLogger()

  private lateinit var eventListeners: ListMultimap<Class<*>, (Self, PlayerEvent) -> Unit>

  private val flattenHierarchyCache = LambdaClassValue { type ->
    val types = TypeToken.of(type).types.rawTypes().asSequence()
      .filter { it !== Any::class.java && !isEventRoot(it) }
      .toImmutableList()
    assert(types.isNotEmpty())
    if (types.size == 1) types[0] else types
  }

  private fun isEventRoot(eventType: Class<*>): Boolean {
    return eventType === PlayerEvent::class.java
      || eventType === PromisedPlayerEvent::class.java
      || eventType === PlayerScheduledEvent::class.java
  }

  private fun getEventListeners(eventType: Class<*>): Iterator<(Self, PlayerEvent) -> Unit> {
    val eventTypes = flattenHierarchyCache.get(eventType)
    if (eventTypes === eventType) {
      return eventListeners.get(eventType).iterator()
    }

    @Suppress("UNCHECKED_CAST")
    eventTypes as List<Class<*>>
    return eventTypes.asSequence().flatMap { eventListeners.get(it) }.iterator()
  }

  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    refresh(injector.getInstancesOfType(PlayerEventListener::class))
  }

  private fun refresh(listeners: Iterable<PlayerEventListener>) {
    val builder = ImmutableListMultimap.builder<Class<*>, (Self, PlayerEvent) -> Unit>()
    for (listener in listeners) {
      for ((eventType, receiver) in listener.playerEventReceive().receive) {
        if (isEventRoot(eventType)) {
          throw IllegalArgumentException("Can not listen a root event type: ${eventType.name}")
        }
        builder.put(eventType, receiver)
      }
    }
    val multiMap = builder.build()
    for ((k, v) in multiMap.asMap()) {
      if (PromisedPlayerEvent::class.java.isAssignableFrom(k) && v.size > 1) {
        throw IllegalStateException("Only one listener allowed for PromisedPlayerEvent: ${k.name}")
      }
    }
    eventListeners = multiMap
  }

  @Deprecated("Don't use, for hotfix only.")
  fun unsafeRegister(listeners: Collection<PlayerEventListener>) {
    val allListeners =
      (injector.getInstancesOfType(PlayerEventListener::class).asSequence() + listeners.asSequence()).toSet()
    refresh(allListeners)
  }

  fun dispatchPromised(self: Self, event: PromisedPlayerEvent<*>) {
    val promise = event.promise
    val listeners = getEventListeners(event.javaClass)
    if (!listeners.hasNext()) {
      promise.failure(NoStackTraceException("No Listener for event: $event"))
      return
    }
    try {
      val listener = listeners.next()
      listener.invoke(self, event)
    } catch (e: Throwable) {
      promise.failure(e)
      logger.error(e) { "事件处理失败: $event" }
      if (e.isFatal()) {
        throw e
      }
    }
  }

  fun dispatch(self: Self, event: PlayerEvent) {
    val listeners = getEventListeners(event.javaClass)
    if (!listeners.hasNext()) {
      logger.debug { "No listener for event: $event" }
      return
    }
    for (listener in listeners) {
      try {
        listener.invoke(self, event)
      } catch (e: Exception) {
        logger.error(e) { "事件处理失败: $event" }
      }
    }
  }
}
