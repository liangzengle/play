package play.example.common.akka.scheduling

import akka.actor.typed.javadsl.ActorContext
import play.util.scheduling.Cancellable
import play.util.scheduling.Scheduler

/**
 * 供Actor内部使用的Cron定时计划
 * @author LiangZengle
 */
class ActorCronScheduler<T> constructor(private val scheduler: Scheduler, private val context: ActorContext<T>) {
  private val schedules = HashMap<String, Cancellable>(4)

  init {
    check(context.classicActorContext().parent().path().name() == "user") { "使用ActorCronScheduler的应该是顶层Actor" }
  }

  fun schedule(cron: String, triggerEvent: T) {
    val future = scheduler.scheduleCron(cron) {
      context.self.tell(triggerEvent)
    }
    schedules.put(cron, future)?.cancel()
  }

  fun cancel(cron: String) {
    schedules.remove(cron)?.cancel()
  }

  fun cancelAll() {
    schedules.values.forEach { it.cancel() }
    schedules.clear()
  }
}
