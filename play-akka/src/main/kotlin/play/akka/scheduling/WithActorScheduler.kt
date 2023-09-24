package play.akka.scheduling

import akka.actor.typed.ActorRef
import play.akka.AbstractTypedActor
import play.scheduling.Trigger
import play.time.Time
import java.time.Duration
import java.time.LocalDateTime

interface WithActorScheduler<T> {
  val actorScheduler: ActorRef<ActorScheduler.Command>

  context(AbstractTypedActor<T>)
  fun schedule(trigger: Trigger, event: T) {
    actorScheduler.tell(ActorScheduler.Schedule(trigger, event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun scheduleAt(triggerTime: LocalDateTime, event: T) {
    actorScheduler.tell(ActorScheduler.ScheduleAt(triggerTime, event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun scheduleAt(triggerTime: Long, event: T) {
    actorScheduler.tell(ActorScheduler.ScheduleAt(Time.toLocalDateTime(triggerTime), event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun scheduleCron(cron: String, event: T) {
    actorScheduler.tell(ActorScheduler.ScheduleCron(cron, event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun scheduleWithTimeout(timeout: Duration, event: T) {
    actorScheduler.tell(ActorScheduler.ScheduleWithTimeout(timeout, event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun cancelSchedule(event: T) {
    actorScheduler.tell(ActorScheduler.Cancel(event, context.self))
  }

  context(AbstractTypedActor<T>)
  fun cancelAllSchedules() {
    actorScheduler.tell(ActorScheduler.CancelAll(context.self))
  }

  context(AbstractTypedActor<T>)
  fun cancelAllSchedules(eventType: Class<out T>) {
    actorScheduler.tell(ActorScheduler.CancelAll(context.self, eventType))
  }
}
