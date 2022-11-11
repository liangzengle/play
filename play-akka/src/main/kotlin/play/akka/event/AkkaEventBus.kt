package play.akka.event

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.javadsl.Behaviors
import play.event.DefaultEventBus
import play.event.EventBus
import play.util.unsafeCast

/**
 * @author LiangZengle
 */
class AkkaEventBus(private val actor: ActorRef<Any>) : EventBus {

  companion object {
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun behavior(): Behavior<Any> {
      val eventBus = DefaultEventBus()
      return Behaviors.receive { ctx, msg ->
        when (msg) {
          is Subscribe -> {
            ctx.system.eventStream().tell(EventStream.Subscribe(msg.eventType as Class<Any>, ctx.self))
            eventBus.subscribe(msg.eventType, msg.subscriber)
          }

          is Publish -> {
            ctx.system.eventStream().tell(EventStream.Publish(msg.event))
          }

          else -> eventBus.publish(msg)
        }
        Behaviors.same()
      }
    }
  }

  override fun publish(event: Any) {
    actor.tell(Publish(event))
  }

  override fun <T> subscribe(eventType: Class<T>, subscriber: (T) -> Unit) {
    actor.tell(Subscribe(eventType, subscriber.unsafeCast()))
  }

  private class Subscribe(val eventType: Class<*>, val subscriber: (Any) -> Unit)

  private class Publish(val event: Any)
}
