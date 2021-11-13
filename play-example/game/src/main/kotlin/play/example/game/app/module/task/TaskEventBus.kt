package play.example.game.app.module.task

import akka.actor.typed.ActorRef
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.list.ImmutableList
import org.springframework.stereotype.Component
import play.example.game.app.module.task.event.TaskEvent

@Component
class TaskEventBus {

  private var subscribers: ImmutableList<Pair<ActorRef<TaskEvent>, (TaskEvent) -> Boolean>> = Lists.immutable.of()

  @Synchronized
  fun subscribe(subscriber: ActorRef<TaskEvent>, eventFilter: (TaskEvent) -> Boolean) {
    subscribers = Lists.immutable.with(subscriber to eventFilter)
  }

  @Synchronized
  fun unsubscribe(subscriber: ActorRef<TaskEvent>) {
    subscribers = subscribers.reject { it.first == subscriber }
  }

  fun post(event: TaskEvent) {
    val subscribers = this.subscribers
    for (i in 0 until subscribers.size()) {
      val (subscriber, filter) = subscribers.get(i)
      if (filter(event)) {
        subscriber.tell(event)
      }
    }
  }
}
