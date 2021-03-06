package play.example.common.akka.scheduling

import akka.actor.typed.javadsl.ActorContext
import play.util.scheduling.Scheduler
import java.util.concurrent.ScheduledFuture

/**
 * 供Actor内部使用的Cron定时计划
 * @author LiangZengle
 */
class ActorCronScheduler<T> constructor(private val scheduler: Scheduler, private val context: ActorContext<T>) {
  private val schedules = HashMap<String, ScheduledFuture<*>>(4)

  init {
    check(context.classicActorContext().parent().path().name() == "user") { "使用ActorCronScheduler的应该是顶层Actor" }
  }

  fun schedule(cron: String, triggerEvent: T) {
    val future = scheduler.scheduleCron(cron) {
      context.self.tell(triggerEvent)
    }
    schedules.put(cron, future)?.cancel(false)
  }

  fun cancel(cron: String) {
    schedules.remove(cron)?.cancel(false)
  }

  fun cancelAll() {
    schedules.values.forEach { it.cancel(false) }
    schedules.clear()
  }
}
