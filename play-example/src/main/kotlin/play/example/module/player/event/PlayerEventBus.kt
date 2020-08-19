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

  fun post(event: PlayerEvent) {
    playerManager.get().tell(event)
  }

  fun postBlocking(self: Self, event: PlayerEvent) {
    dispatcher.get().dispatch(self, event)
  }

  fun postToOnlinePlayers(eventFactory: LongFunction<out PlayerEvent>) {
    onlinePlayerService.foreach {
      post(eventFactory.apply(it))
    }
  }
}
