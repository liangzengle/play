@file:Suppress("NOTHING_TO_INLINE", "unused")

package play.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.javadsl.*
import org.slf4j.Logger
import play.akka.logging.ActorMDC
import play.scala.toJava
import play.scala.toResult
import play.util.concurrent.PlayFuture
import scala.PartialFunction
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

abstract class AbstractTypedActor<T>(context: ActorContext<T>) : AbstractBehavior<T>(context) {
  protected inline val self: ActorRef<T> get() = context.self
  protected inline val log: Logger get() = context.log
  protected inline val ec: ExecutionContextExecutor get() = context.executionContext

  protected inline fun <reified T1 : T> subscribe() {
    context.system.eventStream().tell(EventStream.Subscribe(T1::class.java, self.narrow<T1>()))
  }

  protected inline fun newBehaviorBuilder(): BehaviorBuilder<T> = BehaviorBuilder.create()

  @JvmName("accept")
  protected inline fun <reified T1 : T> ReceiveBuilder<T>.accept(noinline f: (T1) -> Unit): ReceiveBuilder<T> {
    return onMessage(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  @JvmName("apply")
  protected inline fun <reified T1 : T> ReceiveBuilder<T>.accept(noinline f: (T1) -> Behavior<T>): ReceiveBuilder<T> {
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
  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: (T1) -> Behavior<T>): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f(it)
    }
  }

  @JvmName("applySignal")
  protected inline fun <reified T1 : Signal> ReceiveBuilder<T>.acceptSignal(noinline f: () -> Behavior<T>): ReceiveBuilder<T> {
    return onSignal(T1::class.java) {
      f()
    }
  }

  @JvmName("accept")
  protected inline fun <reified T1 : T> BehaviorBuilder<T>.accept(noinline f: (T1) -> Unit): BehaviorBuilder<T> {
    return onMessage(T1::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  @JvmName("accept")
  protected inline fun <reified T1 : T> BehaviorBuilder<T>.accept(f: Runnable): BehaviorBuilder<T> {
    return onMessage(T1::class.java) {
      f.run()
      Behaviors.same()
    }
  }

  @JvmName("apply")
  protected inline fun <reified T1 : T> BehaviorBuilder<T>.accept(noinline f: (T1) -> Behavior<T>): BehaviorBuilder<T> {
    return onMessage(T1::class.java) {
      f(it)
    }
  }

  @JvmName("acceptSignal")
  protected inline fun <reified S : Signal> BehaviorBuilder<T>.acceptSignal(noinline f: (S) -> Unit): BehaviorBuilder<T> {
    return onSignal(S::class.java) {
      f(it)
      Behaviors.same()
    }
  }

  @JvmName("acceptSignal")
  protected inline fun <reified S : Signal> BehaviorBuilder<T>.acceptSignal(f: Runnable): BehaviorBuilder<T> {
    return onSignal(S::class.java) {
      f.run()
      Behaviors.same()
    }
  }

  @JvmName("applySignal")
  protected inline fun <reified S : Signal> BehaviorBuilder<T>.acceptSignal(noinline f: (S) -> Behavior<T>): BehaviorBuilder<T> {
    return onSignal(S::class.java) { f(it) }
  }

  @JvmName("applySignal")
  protected inline fun <reified S : Signal> BehaviorBuilder<T>.acceptSignal(noinline f: () -> Behavior<T>): BehaviorBuilder<T> {
    return onSignal(S::class.java) { f() }
  }

  protected inline fun sameBehavior(): Behavior<T> = Behaviors.same()
  protected inline fun emptyBehavior(): Behavior<T> = Behaviors.empty()
  protected inline fun stoppedBehavior(): Behavior<T> = Behaviors.stopped()
  protected inline fun behaviorBuilder(): BehaviorBuilder<T> = BehaviorBuilder.create()

  protected inline infix fun <A> ActorRef<A>.send(msg: A) = tell(msg)

  protected fun withResumeSupervisor(b: Behavior<T>): Behavior<T> {
    return Behaviors.supervise(b).onFailure(SupervisorStrategy.resume())
  }

  protected inline fun <A> future(noinline f: () -> A): Future<A> = Future.apply(f, ec)

  protected inline fun <A, B> Future<A>.map(noinline f: (A) -> B): Future<B> = map(f, ec)

  protected inline fun <A, B> Future<A>.flatMap(noinline f: (A) -> Future<B>): Future<B> = flatMap(f, ec)

  protected inline fun <A> Future<A>.foreach(noinline f: (A) -> Unit) = foreach(f, ec)

  protected inline fun <A> Future<A>.filter(noinline f: (A) -> Boolean): Future<A> = filter(f, ec)

  protected inline fun <A, B : A> Future<B>.recover(noinline f: (Throwable) -> A): Future<A> =
    recover(PartialFunction.fromFunction(f), ec)

  protected inline fun <A, B : A> Future<B>.recoverWith(noinline f: (Throwable) -> Future<A>): Future<A> =
    recoverWith(PartialFunction.fromFunction(f), ec)

  protected inline fun <A, B> Future<B>.andThen(noinline f: (Result<B>) -> A): Future<B> =
    andThen(PartialFunction.fromFunction { f(it.toResult()) }, ec)

  protected inline fun <A> Future<A>.onComplete(noinline f: (Result<A>) -> Unit) {
    onComplete({ f(it.toResult()) }, ec)
  }

  protected inline fun <A> Future<A>.pipToSelf(noinline resultMapper: (Result<A>) -> T) {
    context.pipeToSelf(this.toJava()) { v, e ->
      resultMapper(if (e != null) Result.failure(e) else Result.success(v))
    }
  }

  protected inline fun <A> PlayFuture<A>.pipToSelf(noinline resultMapper: (Result<A>) -> T) {
    context.pipeToSelf(this.toJava()) { v, e ->
      resultMapper(if (e != null) Result.failure(e) else Result.success(v))
    }
  }

  protected fun <A> spawn(
    name: String, messageType: Class<A>, mdc: ActorMDC, behavior: Behavior<A>
  ): ActorRef<A> {
    return spawn(name, messageType, mdc, SupervisorStrategy.resume(), behavior)
  }

  protected fun <A> spawn(
    name: String,
    messageType: Class<A>,
    mdc: ActorMDC,
    supervisorStrategy: SupervisorStrategy,
    behavior: Behavior<A>
  ): ActorRef<A> {
    return context.spawn(
      Behaviors.supervise(
        Behaviors.withMdc(messageType, mdc.staticMdc, mdc.mdcPerMessage(), behavior)
      ).onFailure(supervisorStrategy),
      name
    )
  }
}
