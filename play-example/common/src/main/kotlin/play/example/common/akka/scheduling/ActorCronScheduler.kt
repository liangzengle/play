package play.example.common.akka.scheduling

import akka.actor.typed.javadsl.ActorContext
import play.example.common.scheduling.ScheduledEvent
import play.scheduling.Cancellable
import play.scheduling.Scheduler

/**
 * 供Actor内部使用的Cron定时计划
 * @author LiangZengle
 */
class ActorCronScheduler<T: Any> constructor(
  private val scheduler: Scheduler,
  private val context: ActorContext<T>
) {
  private val schedules = HashMap<Any, Cancellable>(4)

  init {
    check(context.classicActorContext().parent().path().name() == "user") { "使用ActorCronScheduler的应该是顶层Actor" }
  }

  fun schedule(cron: String, triggerEvent: T) {
    val future = scheduler.scheduleCron(cron) {
      context.self.tell(triggerEvent)
    }
    schedules.put(triggerEvent, future)?.cancel()
  }

  fun cancel(event: T) {
    schedules.remove(event)?.cancel()
  }

  fun cancelAll(eventType: Class<out T>) {
    val eventList = schedules.keys.filter { eventType.isAssignableFrom(it.javaClass) }.toList()
    for (scheduledEvent in eventList) {
      schedules.remove(scheduledEvent)?.cancel()
    }
  }

  fun cancelAll() {
    schedules.values.forEach { it.cancel() }
    schedules.clear()
  }
}
