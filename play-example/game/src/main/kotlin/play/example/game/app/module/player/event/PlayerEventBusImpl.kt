package play.example.game.app.module.player.event

import akka.actor.typed.ActorRef
import com.google.common.collect.Maps
import mu.KLogging
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component
import play.event.EventBusHelper
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerManager.Self
import play.spring.SingletonBeanContext
import play.util.exception.NoStackTraceException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@Component
class PlayerEventBusImpl(private val beanContext: SingletonBeanContext) : PlayerEventBus, PlayerEventDispatcher,
  SmartInitializingSingleton {
  companion object : KLogging()

  private lateinit var playerManager: ActorRef<PlayerManager.Command>

  private val subscribers = Maps.newConcurrentMap<Class<*>, CopyOnWriteArrayList<(Self, Any) -> Unit>>()

  override fun afterSingletonsInstantiated() {
    playerManager = beanContext.getBean()
  }

  override fun dispatch(self: Self, event: PlayerEvent) {
    val subscribers = EventBusHelper.getSubscribers(event.javaClass, subscribers::get)
    if (!subscribers.hasNext()) {
      logger.debug { "No listener for event: $event" }
      if (event is PromisedPlayerEvent<*>) {
        event.promise.failure(NoStackTraceException("No Listener for event: $event"))
      }
      return
    }
    for (subscriber in subscribers) {
      try {
        subscriber(self, event)
      } catch (e: Exception) {
        logger.error(e) { "Exception occurred when handling event: $event" }
        if (event is PromisedPlayerEvent<*>) {
          event.promise.failure(e)
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> subscribe(eventType: Class<T>, action: (Self, T) -> Unit) {
    subscribers.computeIfAbsent(eventType) { CopyOnWriteArrayList() }.add(action as (Self, Any) -> Unit)
  }

  override fun publish(event: PlayerEvent) {
    playerManager.tell(event)
  }

  override fun publishSync(self: Self, event: PlayerEvent) {
    dispatch(self, event)
  }
}
