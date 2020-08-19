package play.example.game.app.module.modulecontrol.event

import play.example.game.app.module.player.event.PlayerEvent

data class PlayerModuleOpenEvent(override val playerId: Long, val moduleId: Int) : PlayerEvent
