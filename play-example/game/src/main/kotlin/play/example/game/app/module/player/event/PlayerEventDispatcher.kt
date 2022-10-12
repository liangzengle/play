package play.example.game.app.module.player.event

import play.example.game.app.module.player.PlayerManager.Self

interface PlayerEventDispatcher {

  fun dispatch(self: Self, event: PlayerEvent)
}
