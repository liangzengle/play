package play.example.game.app.module.player.event

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.event.AbstractPlayerTaskEvent
import kotlin.reflect.KClass

interface PlayerEventBus {
  fun publish(event: PlayerEvent)

  fun publishSync(self: Self, event: PlayerEvent)

  fun publish(playerId: Long, event: AbstractPlayerTaskEvent) {
    publish(PlayerTaskEvent(playerId, event))
  }

  fun publish(self: Self, event: AbstractPlayerTaskEvent) {
    publish(self.id, event)
  }

  fun <T> subscribe(eventType: Class<T>, action: (Self) -> Unit) {
    subscribe(eventType) { self, _ ->
      action(self)
    }
  }

  fun <T> subscribe(eventType: Class<T>, action: (Self, T) -> Unit)

  fun <T : Any> subscribe(eventType: KClass<T>, action: (Self) -> Unit) {
    subscribe(eventType) { self, _ ->
      action(self)
    }
  }

  fun <T : Any> subscribe(eventType: KClass<T>, action: (Self, T) -> Unit) {
    subscribe(eventType.java, action)
  }
}

inline fun <reified T> PlayerEventBus.subscribe(noinline action: (Self, T) -> Unit) {
  subscribe(T::class.java, action)
}

inline fun <reified T> PlayerEventBus.subscribe(noinline action: (Self) -> Unit) {
  subscribe(T::class.java, action)
}

inline fun <reified T> PlayerEventBus.subscribe(noinline action: () -> Unit) {
  subscribe(T::class.java) { _ -> action() }
}
