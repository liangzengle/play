package play.example.game.app.module.player.event

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.event.IPlayerTaskEvent
import play.util.concurrent.Future
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import kotlin.reflect.KClass

interface PlayerEventBus {
  fun publish(event: PlayerEvent)

  fun publishSync(self: Self, event: PlayerEvent)

  fun publish(playerId: Long, event: IPlayerTaskEvent) {
    if (event is PlayerEvent) {
      publish(event as PlayerEvent)
    } else {
      publish(PlayerTaskEvent(playerId, event))
    }
  }

  fun publish(self: Self, event: IPlayerTaskEvent) {
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

  private data class PlayerTaskEvent(override val playerId: Long, override val taskEvent: IPlayerTaskEvent) :
    PlayerTaskEventLike
}

inline fun <reified T> PlayerEventBus.subscribe(noinline action: (Self, T) -> Unit) {
  subscribe(T::class.java, action)
}

inline fun <reified T> PlayerEventBus.subscribe(noinline action: (Self) -> Unit) {
  subscribe(T::class.java, action)
}

inline fun <reified T> PlayerEventBus.subscribe0(noinline action: () -> Unit) {
  subscribe(T::class.java) { _ -> action() }
}

context(PlayerEventBus)
fun <T> PlayFuture<T>.pipeToPlayer(eventMapper: (Result<T>) -> PlayerEvent) {
  onComplete { publish(eventMapper(it)) }
}

context(PlayerEventBus)
fun <T, U> PlayFuture<T>.pipeToPlayer(eventMapper: (Result<T>, PlayPromise<U>) -> PromisedPlayerEvent<U>): Future<U> {
  val promise = PlayPromise.make<U>()
  onComplete {
    val event = eventMapper(it, promise)
    publish(event)
  }
  return promise.future
}
