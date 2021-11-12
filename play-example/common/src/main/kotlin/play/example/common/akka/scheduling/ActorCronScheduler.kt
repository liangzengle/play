package play.example.common.akka.scheduling

import akka.actor.typed.javadsl.ActorContext
import play.scheduling.Cancellable
import play.scheduling.Scheduler

/**
 * 供Actor内部使用的Cron定时计划
 * @author LiangZengle
 */
class ActorCronScheduler<T : Any> constructor(
  private val scheduler: Scheduler,
  private val context: ActorContext<T>
) {
  private val schedules = HashMap<Any, Cancellable>(4)

  /**
   * 添加一个定时任务
   *
   * @param cron cron表达式
   * @param triggerEvent 触发时发送的事件，非单例对象需要实现hashCode和equals
   */
  fun schedule(cron: String, triggerEvent: T) {
    val actorRef = context.self // avoid lambda capture `this`
    val future = scheduler.scheduleCron(cron) {
      actorRef.tell(triggerEvent)
    }
    schedules.put(triggerEvent, future)?.cancel()
  }

  /**
   * 取消定时任务
   *
   * @param event [schedule]添加定时任务时使用的事件
   */
  fun cancel(event: T) {
    schedules.remove(event)?.cancel()
  }

  /**
   * 取消某种类型的定时任务
   *
   * @param eventType 事件类型
   */
  fun cancelAll(eventType: Class<out T>) {
    val eventList = schedules.keys.filter { eventType.isAssignableFrom(it.javaClass) }
    for (scheduledEvent in eventList) {
      schedules.remove(scheduledEvent)?.cancel()
    }
  }

  /**
   * 取消所有定时任务
   */
  fun cancelAll() {
    schedules.values.forEach { it.cancel() }
    schedules.clear()
  }
}
