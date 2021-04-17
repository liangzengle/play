package play.example.game.module.server.event

import play.example.game.module.player.event.PlayerEvent

/**
 * 服务器启动完毕时间
 */
object ApplicationStartedEvent

/**
 * 开服事件
 */
object ServerOpenEvent

/**
 * 玩家的开服事件
 * @property playerId 玩家id
 */
data class ServerOpenPlayerEvent(override val playerId: Long) : PlayerEvent
