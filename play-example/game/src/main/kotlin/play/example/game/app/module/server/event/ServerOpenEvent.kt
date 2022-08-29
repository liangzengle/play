package play.example.game.app.module.server.event

import play.example.game.app.module.player.event.PlayerEvent

/**
 * 开服事件
 */
object ServerOpenEvent

/**
 * 玩家的开服事件
 * @property playerId 玩家id
 */
data class ServerOpenPlayerEvent(override val playerId: Long) : PlayerEvent
