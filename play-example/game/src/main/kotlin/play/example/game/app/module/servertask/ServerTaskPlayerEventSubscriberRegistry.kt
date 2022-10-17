package play.example.game.app.module.servertask

import org.springframework.stereotype.Component
import play.example.game.app.module.player.event.PlayerDayFirstLoginEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.example.game.app.module.player.event.subscribe0
import play.example.game.app.module.servertask.event.ServerLoginEvent

/**
 *
 *
 * @author LiangZengle
 */
@Component
class ServerTaskPlayerEventSubscriberRegistry(
  playerEventBus: PlayerEventBus,
  serverTaskEventBus: ServerTaskEventBus
) {
  init {
    playerEventBus.subscribe0<PlayerDayFirstLoginEvent> { serverTaskEventBus.post(ServerLoginEvent) }
  }
}
