package play.example.common.akka.scheduling

import akka.actor.typed.ActorRef
import play.util.scheduling.Scheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Actor的Cron定时计划
 * @author LiangZengle
 */
@Singleton
class ActorCronScheduler @Inject constructor(private val scheduler: Scheduler) {
  private val schedules = ConcurrentHashMap<Any, ScheduledFuture<*>>()

  fun <T> schedule(key: Any, cron: String, triggerEvent: T, receiver: ActorRef<T>) {
    val future = scheduler.scheduleCron(cron) {
      receiver.tell(triggerEvent)
    }
    val prev = schedules.put(key, future)
    prev?.cancel(false)
  }

  fun cancel(key: Any) {
    schedules[key]?.cancel(false)
  }
}
