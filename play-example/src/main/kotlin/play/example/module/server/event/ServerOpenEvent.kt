package play.example.module.server.event

import play.example.common.event.ApplicationEvent
import play.example.module.player.event.PlayerEvent

object ServerOpenEvent : ApplicationEvent

data class ServerOpenPlayerEvent(override val playerId: Long) : PlayerEvent
