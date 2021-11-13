package play.example.game.app.module.player.event

import akka.actor.typed.ActorRef
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import play.example.game.app.module.player.OnlinePlayerService
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.Self
import play.example.game.app.module.task.event.TaskEvent
import java.util.function.LongFunction

@Component
class PlayerEventBus(
  private val playerManager: ObjectProvider<ActorRef<PlayerManager.Command>>,
  private val onlinePlayerService: OnlinePlayerService,
  private val dispatcher: ObjectProvider<PlayerEventDispatcher>
) {
  private var _playerManager: ActorRef<PlayerManager.Command>? = null
  private var _dispatcher: PlayerEventDispatcher? = null

  fun post(event: PlayerEvent) {
    if (_playerManager == null) {
      _playerManager = playerManager.getObject()
    }
    _playerManager!!.tell(event)
  }

  fun post(playerId: Long, event: TaskEvent) {
    post(PlayerTaskEvent(playerId, event))
  }

  fun post(self: Self, event: TaskEvent) {
    post(self.id, event)
  }

  fun postSync(self: Self, event: PlayerEvent) {
    if (_dispatcher == null) {
      _dispatcher = dispatcher.getObject()
    }
    _dispatcher!!.dispatch(self, event)
  }

  fun postToOnlinePlayers(eventFactory: LongFunction<out PlayerEvent>) {
    onlinePlayerService.onLinePlayerIdIterator().forEach { post(eventFactory.apply(it)) }
  }
}
