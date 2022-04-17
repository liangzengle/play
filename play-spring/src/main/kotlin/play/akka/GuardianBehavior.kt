package play.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import play.util.concurrent.PlayPromise
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
object GuardianBehavior {
  interface Command

  data class Spawn<T>(val behavior: Behavior<T>, val name: String, val promise: PlayPromise<ActorRef<T>>) : Command

  val behavior: Behavior<Command> = Behaviors.receive { ctx, cmd ->
    when (cmd) {
      is Spawn<*> -> {
        val msg = cmd.unsafeCast<Spawn<Any>>()
        msg.promise.catchingComplete { ctx.spawn(msg.behavior, cmd.name) }
        Behaviors.same()
      }
      else -> Behaviors.same()
    }
  }
}
