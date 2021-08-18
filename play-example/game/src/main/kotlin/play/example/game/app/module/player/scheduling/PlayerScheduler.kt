package play.example.game.app.module.player.scheduling

import akka.actor.typed.ActorRef
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEvent
import play.util.time.currentMillis
import play.util.time.toMillis
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * 玩家定时器
 * @author LiangZengle
 */
@Singleton
@Named
class PlayerScheduler @Inject constructor(private val playerManager: Provider<ActorRef<PlayerManager.Command>>) {

  fun schedule(schedule: PlayerSchedule) {
    playerManager.get().tell(schedule)
  }

  /**
   * 玩家定时计划
   * @param playerId 玩家id
   * @param triggerEvent 定时触发的事件
   * @param triggerTimeMillis 触发时间(毫秒)
   */
  fun scheduleAt(
    playerId: Long,
    triggerEvent: PlayerEvent,
    triggerTimeMillis: Long
  ) {
    playerManager.get().tell(PlayerSchedule(playerId, triggerEvent, triggerTimeMillis))
  }

  /**
   * 玩家定时计划
   * @param playerId 玩家id
   * @param triggerEvent 定时触发的事件
   * @param triggerTime 触发时间
   */
  fun scheduleAt(
    playerId: Long,
    triggerEvent: PlayerEvent,
    triggerTime: LocalDateTime
  ) = scheduleAt(playerId, triggerEvent, triggerTime.toMillis())

  /**
   * 玩家定时计划
   * @param playerId 玩家id
   * @param triggerEvent 定时触发的事件
   * @param delay 触发延迟
   */
  fun schedule(
    playerId: Long,
    triggerEvent: PlayerEvent,
    delay: Duration
  ) = scheduleAt(playerId, triggerEvent, currentMillis() + delay.inWholeMilliseconds)

  /**
   * 玩家的循环定时计划
   * @param playerId 玩家id
   * @param triggerEvent 定时触发的事件
   * @param interval 触发间隔
   * @param triggerImmediately 是否立即触发1次
   */
  fun scheduleRepeatedly(
    playerId: Long,
    triggerEvent: PlayerEvent,
    interval: Duration,
    triggerImmediately: Boolean = false
  ) {
    val schedule = PlayerRepeatedSchedule(playerId, triggerEvent, interval.toJavaDuration(), triggerImmediately)
    playerManager.get().tell(schedule)
  }

  /**
   * 取消定时计划
   * @param triggerEvent 定时计划触发的事件
   */
  fun cancel(triggerEvent: PlayerEvent) {
    playerManager.get().tell(PlayerScheduleCancellation(triggerEvent.playerId, triggerEvent))
  }
}
