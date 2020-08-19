package play.example.module.player.event

import play.example.module.player.PlayerActor
import play.example.module.player.PlayerManager

interface PlayerEvent : PlayerManager.Command, PlayerActor.Command {
  val playerId: Long
}

data class PlayerRequestEvent(override val playerId: Long, val message: PlayerActor.RequestCommand) : PlayerEvent

/**
 * 玩家登录前
 */
data class PlayerPreLoginEvent(override val playerId: Long) : PlayerEvent

/**
 * 玩家登录后
 */
data class PlayerLoginEvent(override val playerId: Long) : PlayerEvent

/**
 * 玩家登出
 */
data class PlayerLogoutEvent(override val playerId: Long) : PlayerEvent

/**
 * 玩家跨天
 */
data class PlayerNewDayStartEvent(override val playerId: Long) : PlayerEvent

/**
 * 玩家跨周
 */
data class PlayerNewWeekStartEvent(override val playerId: Long) : PlayerEvent

/**
 * 玩家跨月
 */
data class PlayerNewMonthStartEvent(override val playerId: Long) : PlayerEvent
