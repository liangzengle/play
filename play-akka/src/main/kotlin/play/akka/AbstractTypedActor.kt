@file:Suppress("NOTHING_TO_INLINE")

package play.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.*
import org.slf4j.Logger

abstract class AbstractTypedActor<T>(context: ActorContext<T>) : AbstractBehavior<T>(context) {
  protected inline val self: ActorRef<T> get() = context.self
  protected inline val log: Logger get() = context.log

  inline fun <reified T1 : T, T> ReceiveBuilder<T>.accept(noinline f: (T1) -> Unit): ReceiveBuilder<T> {
    return onMessage(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  inline fun <reified T1 : T, T> ReceiveBuilder<T>.apply(noinline f: (T1) -> Behavior<T>): ReceiveBuilder<T> {
    return onMessage(T1::class.java, f)
  }

  inline fun <reified T1 : Signal> ReceiveBuilder<T>.onSignal(noinline f: (T1) -> Unit): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  inline fun <reified T1 : Signal> ReceiveBuilder<T>.onSignal(noinline f: () -> Unit): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f()
      Behaviors.same()
    }
  }
}

inline fun <T> AbstractTypedActor<T>.sameBehavior() = Behaviors.same<T>()

inline fun <T> AbstractTypedActor<T>.emptyBehavior() = Behaviors.empty<T>()

inline fun <T> AbstractTypedActor<T>.stoppedBehavior() = Behaviors.stopped<T>()

inline fun <T> AbstractTypedActor<T>.behaviorBuilder() = BehaviorBuilder.create<T>()

inline infix fun <T> ActorRef<T>.send(msg: T) = tell(msg)

fun <T> resumeSupervisor(b: Behavior<T>): Behavior<T> {
  return Behaviors.supervise(b).onFailure(SupervisorStrategy.resume())
}
