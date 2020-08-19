package play.example.game.app.module.player.event

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.inject.PlayInjector
import play.spring.OrderedSmartInitializingSingleton
import play.util.exception.NoStackTraceException
import play.util.exception.isFatal
import play.util.isAbstract
import play.util.isAssignableFrom
import play.util.logging.getLogger
import play.util.unsafeCast

@Component
class PlayerEventDispatcher(private val injector: PlayInjector) :
  OrderedSmartInitializingSingleton {
  private val logger = getLogger()

  private lateinit var oneForOne: Map<Class<PlayerEvent>, List<(Self, PlayerEvent) -> Unit>>
  private lateinit var oneForMany: List<Pair<Class<PlayerEvent>, List<(Self, PlayerEvent) -> Unit>>>

  override fun afterSingletonsInstantiated(beanFactory: BeanFactory) {
    val oneForOne = hashMapOf<Class<PlayerEvent>, MutableList<(Self, PlayerEvent) -> Unit>>()
    val oneForManyBuilder = ImmutableListMultimap.builder<Class<PlayerEvent>, (Self, PlayerEvent) -> Unit>()
    for (listener in injector.getInstancesOfType(PlayerEventListener::class)) {
      for ((eventType, receiver) in listener.playerEventReceive().receive) {
        if (eventType.isAbstract()) {
          if (isAssignableFrom<PromisedPlayerEvent<*>>(eventType)) {
            throw IllegalStateException("Possible Multi Listeners for PromisedPlayerEvent: ${eventType.name}")
          }
          oneForManyBuilder.put(eventType, receiver)
        } else {
          val list = oneForOne.computeIfAbsent(eventType) { ArrayList(2) }
          list.add(receiver)
        }
      }
    }
    this.oneForOne = ImmutableMap.copyOf(oneForOne)
    this.oneForMany = oneForManyBuilder.build().asMap().entries.map { it.key to it.value.unsafeCast() }
  }

  fun dispatchPromised(self: Self, event: PromisedPlayerEvent<*>) {
    val promise = event.promise
    val listeners = oneForOne[event.javaClass]
    if (listeners.isNullOrEmpty()) {
      promise.failure(NoStackTraceException("No Listener for event: $event"))
      return
    }
    if (listeners.size > 1) {
      promise.failure(NoStackTraceException("More than 1 Listener for event: $event"))
      return
    }
    try {
      listeners[0](self, event)
    } catch (e: Throwable) {
      promise.failure(e)
      logger.error(e) { "事件处理失败: $event" }
      if (e.isFatal()) {
        throw e
      }
    }
  }

  fun dispatch(self: Self, event: PlayerEvent) {
    val listeners = oneForOne[event.javaClass]
    if (listeners != null) {
      for (i in listeners.indices) {
        try {
          listeners[i](self, event)
        } catch (e: Exception) {
          logger.error(e) { "事件处理失败: $event" }
        }
      }
    }
    for (i in oneForMany.indices) {
      val (clazz, list) = oneForMany[i]
      if (!clazz.isAssignableFrom(event.javaClass)) {
        continue
      }
      for (j in list.indices) {
        try {
          list[j](self, event)
        } catch (e: Exception) {
          logger.error(e) { "事件处理失败: $event" }
        }
      }
    }
  }
}
