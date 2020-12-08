@file:Suppress("NOTHING_TO_INLINE", "unused")

package play.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.javadsl.*
import org.slf4j.Logger
import play.scala.toJava
import play.util.concurrent.PlayFuture
import scala.PartialFunction
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

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

  protected inline fun <T> future(noinline f: () -> T): Future<T> = Future.apply(f, ec)

  protected inline fun <T, S> Future<T>.map(noinline f: (T) -> S): Future<S> = map(f, ec)

  protected inline fun <T, S> Future<T>.flatMap(noinline f: (T) -> Future<S>): Future<S> = flatMap(f, ec)

  protected inline fun <T> Future<T>.foreach(noinline f: (T) -> Unit) = foreach(f, ec)

  protected inline fun <T> Future<T>.filter(noinline f: (T) -> Boolean): Future<T> = filter(f, ec)

  protected inline fun <U, T : U> Future<T>.recover(noinline f: (Throwable) -> U): Future<U> =
    recover(PartialFunction.fromFunction(f), ec)

  protected inline fun <U, T : U> Future<T>.recoverWith(noinline f: (Throwable) -> Future<U>): Future<U> =
    recoverWith(PartialFunction.fromFunction(f), ec)

  protected inline fun <U, T> Future<T>.andThen(noinline f: (Result<T>) -> U): Future<T> =
    andThen(
      PartialFunction.fromFunction {
        when (it) {
          is Success<T> -> f(Result.success(it.value()))
          is Failure<T> -> f(Result.failure(it.exception()))
          else -> error("won't happen")
        }
      },
      ec
    )

  protected inline fun <T> Future<T>.onComplete(noinline f: (Result<T>) -> Unit) {
    onComplete(
      {
        when (it) {
          is Success<T> -> f(Result.success(it.value()))
          is Failure<T> -> f(Result.failure(it.exception()))
          else -> error("won't happen")
        }
      },
      ec
    )
  }

  protected inline fun <U> Future<U>.pipToSelf(noinline resultMapper: (Result<U>) -> T) {
    context.pipeToSelf(this.toJava()) { v, e ->
      resultMapper(if (e != null) Result.failure(e) else Result.success(v))
    }
  }

  protected inline fun <U> PlayFuture<U>.pipToSelf(noinline resultMapper: (Result<U>) -> T) {
    context.pipeToSelf(this.toJava()) { v, e ->
      resultMapper(if (e != null) Result.failure(e) else Result.success(v))
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
