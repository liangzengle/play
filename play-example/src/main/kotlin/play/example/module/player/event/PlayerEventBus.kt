package play.example.module.player.event

import akka.actor.typed.ActorRef
import play.example.module.player.OnlinePlayerService
import play.example.module.player.PlayerManager
import play.example.module.player.Self
import java.util.function.LongFunction
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PlayerEventBus @Inject constructor(
  private val playerManager: Provider<ActorRef<PlayerManager.Command>>,
  private val onlinePlayerService: OnlinePlayerService,
  private val dispatcher: PlayerEventDispatcherProvider
) {
  // is it necessary to avoid volatile read?
  private var _playerManager: ActorRef<PlayerManager.Command>? = null
  private var _dispatcher: PlayerEventDispatcher? = null

  fun post(event: PlayerEvent) {
    if (_playerManager == null) {
      _playerManager = playerManager.get()
    }
    _playerManager!!.tell(event)
  }

  fun postBlocking(self: Self, event: PlayerEvent) {
    if (_dispatcher == null) {
      _dispatcher = dispatcher.get()
    }
    _dispatcher!!.dispatch(self, event)
  }

  fun postToOnlinePlayers(eventFactory: LongFunction<out PlayerEvent>) {
    onlinePlayerService.foreach {
      post(eventFactory.apply(it))
    }
  }
}
