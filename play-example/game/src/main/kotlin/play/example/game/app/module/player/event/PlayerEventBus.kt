package play.example.game.app.module.player.event

import akka.actor.typed.ActorRef
import org.springframework.beans.factory.ObjectProvider
import play.example.game.app.module.player.OnlinePlayerService
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.Self
import play.example.game.app.module.task.event.TaskEvent
import java.util.function.LongFunction
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Named
class PlayerEventBus @Inject constructor(
  private val playerManager: Provider<ActorRef<PlayerManager.Command>>,
  private val onlinePlayerService: OnlinePlayerService,
  private val dispatcher: ObjectProvider<PlayerEventDispatcher>
) {
  private var _playerManager: ActorRef<PlayerManager.Command>? = null
  private var _dispatcher: PlayerEventDispatcher? = null

  fun post(event: PlayerEvent) {
    if (_playerManager == null) {
      _playerManager = playerManager.get()
    }
    _playerManager!!.tell(event)
  }

  fun post(playerId: Long, event: TaskEvent) {
    post(PlayerTaskEvent(playerId, event))
  }

  fun post(self: Self, event: TaskEvent) {
    post(self.id, event)
  }

  fun postBlocking(self: Self, event: PlayerEvent) {
    if (_dispatcher == null) {
      _dispatcher = dispatcher.getObject()
    }
    _dispatcher!!.dispatch(self, event)
  }

  fun postToOnlinePlayers(eventFactory: LongFunction<out PlayerEvent>) {
    onlinePlayerService.foreach {
      post(eventFactory.apply(it))
    }
  }
}
