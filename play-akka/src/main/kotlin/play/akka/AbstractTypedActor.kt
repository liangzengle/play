@file:Suppress("NOTHING_TO_INLINE", "unused")

package play.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.javadsl.*
import org.slf4j.Logger
import scala.concurrent.ExecutionContext

abstract class AbstractTypedActor<T>(context: ActorContext<T>) : AbstractBehavior<T>(context) {
  protected inline val self: ActorRef<T> get() = context.self
  protected inline val log: Logger get() = context.log
  protected inline val ec: ExecutionContext get() = context.executionContext

  protected inline fun <reified T1 : T> subscribe() {
    context.system.eventStream().tell(EventStream.Subscribe(T1::class.java, self.unsafeUpcast()))
  }

  protected inline fun newBehaviorBuilder(): BehaviorBuilder<T> = BehaviorBuilder.create()

  @JvmName("accept")
  @OverloadResolutionByLambdaReturnType
  protected inline fun <reified T1 : T, T> ReceiveBuilder<T>.accept(noinline f: (T1) -> Unit): ReceiveBuilder<T> {
    return onMessage(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  @JvmName("apply")
  @OverloadResolutionByLambdaReturnType
  protected inline fun <reified T1 : T, T> ReceiveBuilder<T>.accept(noinline f: (T1) -> Behavior<T>): ReceiveBuilder<T> {
    return onMessage(T1::class.java, f)
  }

  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: (T1) -> Unit): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: () -> Unit): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f()
      Behaviors.same()
    }
  }

  @JvmName("applySignal")
  @OverloadResolutionByLambdaReturnType
  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: (T1) -> Behavior<T>): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f(it)
    }
  }

  @JvmName("applySignal")
  @OverloadResolutionByLambdaReturnType
  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: () -> Behavior<T>): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f()
    }
  }
}

@JvmName("accept")
@OverloadResolutionByLambdaReturnType
inline fun <reified T1 : T, T> BehaviorBuilder<T>.accept(noinline f: (T1) -> Unit): BehaviorBuilder<T> {
  return onMessage(T1::class.java) {
    f(it)
    Behaviors.same()
  }
}

@JvmName("apply")
@OverloadResolutionByLambdaReturnType
inline fun <reified T1 : T, T> BehaviorBuilder<T>.accept(noinline f: (T1) -> Behavior<T>): BehaviorBuilder<T> {
  return onMessage(T1::class.java) {
    f(it)
  }
}

@JvmName("acceptSignal")
@OverloadResolutionByLambdaReturnType
inline fun <reified S : Signal, T> BehaviorBuilder<T>.acceptSignal(noinline f: (S) -> Unit): BehaviorBuilder<T> {
  return onSignal(S::class.java) {
    f(it)
    Behaviors.same()
  }
}

@JvmName("acceptSignal")
@OverloadResolutionByLambdaReturnType
inline fun <reified S : Signal, T> BehaviorBuilder<T>.acceptSignal(noinline f: () -> Unit): BehaviorBuilder<T> {
  return onSignal(S::class.java) {
    f()
    Behaviors.same()
  }
}

@JvmName("applySignal")
@OverloadResolutionByLambdaReturnType
inline fun <reified S : Signal, T> BehaviorBuilder<T>.acceptSignal(noinline f: (S) -> Behavior<T>): BehaviorBuilder<T> {
  return onSignal(S::class.java) { f(it) }
}

@JvmName("applySignal")
@OverloadResolutionByLambdaReturnType
inline fun <reified S : Signal, T> BehaviorBuilder<T>.acceptSignal(noinline f: () -> Behavior<T>): BehaviorBuilder<T> {
  return onSignal(S::class.java) { f() }
}

inline fun <T> AbstractTypedActor<T>.sameBehavior(): Behavior<T> = Behaviors.same()

inline fun <T> AbstractTypedActor<T>.emptyBehavior(): Behavior<T> = Behaviors.empty()

inline fun <T> AbstractTypedActor<T>.stoppedBehavior(): Behavior<T> = Behaviors.stopped()

inline fun <T> AbstractTypedActor<T>.behaviorBuilder(): BehaviorBuilder<T> = BehaviorBuilder.create()

inline infix fun <T> ActorRef<T>.send(msg: T) = tell(msg)

fun <T> resumeSupervisor(b: Behavior<T>): Behavior<T> {
  return Behaviors.supervise(b).onFailure(SupervisorStrategy.resume())
}
