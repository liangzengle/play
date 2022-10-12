package play.example.game.app.module.player.scheduling

import org.jctools.maps.NonBlockingHashMapLong
import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventBus
import play.scheduling.Cancellable
import play.scheduling.Scheduler
import play.util.time.Time
import play.util.time.Time.currentMillis
import play.util.time.Time.toMillis
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * 玩家定时器
 * @author LiangZengle
 */
@Component
class PlayerScheduler(
  private val scheduler: Scheduler,
  private val playerEventBus: PlayerEventBus
) : AutoCloseable {

  private val scheduleMap = NonBlockingHashMapLong<WeakHashMap<PlayerEvent, Cancellable>>()

  private fun getPlayerSchedules(self: Self): MutableMap<PlayerEvent, Cancellable> {
    var map = scheduleMap.get(self.id)
    if (map == null) {
      map = scheduleMap.computeIfAbsent(self.id) { WeakHashMap() }
    }
    return map
  }

  /**
   * 玩家定时计划，在某个事件点触发
   *
   * @param self
   * @param triggerEvent 定时触发的事件
   * @param triggerTimeMillis 触发时间(毫秒)
   */
  fun scheduleAt(
    self: Self,
    triggerEvent: PlayerEvent,
    triggerTimeMillis: Long
  ) {
    val cancellable = scheduler.scheduleAt(Time.toLocalDateTime(triggerTimeMillis)) {
      playerEventBus.publish(triggerEvent)
    }
    val playerSchedules = getPlayerSchedules(self)
    playerSchedules[triggerEvent] = cancellable
  }

  /**
   * 玩家定时计划，在某个事件点触发
   *
   * @param self
   * @param triggerEvent 定时触发的事件
   * @param triggerTime 触发时间
   */
  fun scheduleAt(
    self: Self,
    triggerEvent: PlayerEvent,
    triggerTime: LocalDateTime
  ) = scheduleAt(self, triggerEvent, triggerTime.toMillis())

  /**
   * 玩家定时计划，在一定延迟后触发
   *
   * @param self
   * @param triggerEvent 定时触发的事件
   * @param delay 触发延迟
   */
  fun schedule(
    self: Self,
    triggerEvent: PlayerEvent,
    delay: Duration
  ) = scheduleAt(self, triggerEvent, currentMillis() + delay.toMillis())

  /**
   * 玩家的循环定时计划
   *
   * @param self
   * @param triggerEvent 定时触发的事件
   * @param interval 触发间隔
   */
  fun scheduleRepeatedly(
    self: Self,
    triggerEvent: PlayerEvent,
    interval: Duration,
  ) {
    scheduleRepeatedly(self, triggerEvent, interval, interval)
  }

  /**
   * 玩家的循环定时计划
   *
   * @param self
   * @param triggerEvent 定时触发的事件
   * @param initialDelay 首次触发延迟
   * @param interval 触发间隔
   */
  fun scheduleRepeatedly(
    self: Self,
    triggerEvent: PlayerEvent,
    initialDelay: Duration,
    interval: Duration
  ) {
    val cancellable = scheduler.scheduleWithFixedDelay(initialDelay, interval) {
      playerEventBus.publish(triggerEvent)
    }
    val playerSchedules = getPlayerSchedules(self)
    playerSchedules[triggerEvent] = cancellable
  }

  /**
   * 取消定时计划
   *
   * @param self
   * @param triggerEvent 定时计划触发的事件
   */
  fun cancel(self: Self, triggerEvent: PlayerEvent) {
    val map = scheduleMap.get(self.id) ?: return
    map.remove(triggerEvent)?.cancel()
  }

  /**
   * 取消玩家某个类型的定时计划
   *
   * @param self
   * @param eventType
   */
  fun cancelAll(self: Self, eventType: Class<*>) {
    val map = scheduleMap.get(self.id) ?: return
    map.entries.removeIf { eventType.isAssignableFrom(it.key.javaClass) }
  }

  /**
   * 取消玩家的所有定时计划
   *
   * @param self
   */
  fun cancelAll(self: Self) {
    val map = scheduleMap.remove(self.id) ?: return
    map.values.forEach { it.cancel() }
  }

  override fun close() {
    scheduleMap.values.forEach { map -> map.values.forEach { it.cancel() } }
  }
}
