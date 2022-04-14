package play.example.game.app.module.player.event

import play.example.common.scheduling.ScheduledEvent
import play.example.game.app.module.player.PlayerActor
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.playertask.event.AbstractPlayerTaskEvent
import play.example.game.app.module.reward.model.Cost
import play.example.game.app.module.reward.model.CostResultSet
import play.util.concurrent.PlayPromise
import play.util.concurrent.Promise
import play.util.control.Result2

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

data class PlayerExecCost(
  override val playerId: Long,
  val costs: List<Cost>,
  val logSource: Int,
  override val promise: PlayPromise<Result2<CostResultSet>>
) : PromisedPlayerEvent<Result2<CostResultSet>>

data class PlayerTaskEvent(override val playerId: Long, val taskEvent: AbstractPlayerTaskEvent) : PlayerEvent

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


data class PlayerLevelUpEvent(override val playerId: Long, val oldLv: Int, val newLv: Int) : PlayerEvent
