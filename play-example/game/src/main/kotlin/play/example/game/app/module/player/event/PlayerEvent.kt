package play.example.game.app.module.player.event

import play.example.common.scheduling.ScheduledEvent
import play.example.game.app.module.player.PlayerActor
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.playertask.event.IPlayerTaskEvent
import play.example.game.app.module.task.domain.TaskTargetType
import play.util.concurrent.Promise

/**
 * 玩家事件接口
 * @property playerId 玩家id
 */
interface PlayerEvent : PlayerManager.Command, PlayerActor.Command {
  val playerId: Long
}

interface PlayerScheduledEvent : PlayerEvent, ScheduledEvent

interface PromisedPlayerEvent<T> : PlayerEvent {
  val promise: Promise<T>
}

interface PlayerTaskEventLike : PlayerEvent {
  val taskEvent: IPlayerTaskEvent
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
 * 玩家每天首次登录事件
 */
data class PlayerDayFirstLoginEvent(override val playerId: Long) : PlayerTaskEventLike, IPlayerTaskEvent {
  override val targetType: TaskTargetType
    get() = PlayerTaskTargetType.PlayerLogin

  override val taskEvent: IPlayerTaskEvent
    get() = this
}

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


data class PlayerLevelUpEvent(override val playerId: Long, val oldLv: Int, val newLv: Int) : PlayerEvent
