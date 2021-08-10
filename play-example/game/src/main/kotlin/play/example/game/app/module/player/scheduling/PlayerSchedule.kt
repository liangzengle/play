package play.example.game.app.module.player.scheduling

import java.time.Duration
import play.example.game.app.module.player.event.PlayerEvent

/**
 * 玩家定时计划
 * @property playerId 玩家id
 * @property triggerEvent 定时触发的事件
 * @property triggerTimeMillis 触发时间（毫秒表示）
 */
data class PlayerSchedule(
  override val playerId: Long,
  val triggerEvent: PlayerEvent,
  val triggerTimeMillis: Long
) : PlayerEvent {
  init {
    assert(triggerEvent !is PlayerSchedule)
  }
}

data class PlayerRepeatedSchedule(
  override val playerId: Long,
  val triggerEvent: PlayerEvent,
  val interval: Duration,
  val triggerImmediately: Boolean
) : PlayerEvent {
  init {
    assert(triggerEvent !is PlayerSchedule)
    require(interval > Duration.ZERO) { "Illegal schedule interval: $interval" }
  }
}

data class PlayerScheduleCancellation(override val playerId: Long, val triggerEvent: PlayerEvent) : PlayerEvent
