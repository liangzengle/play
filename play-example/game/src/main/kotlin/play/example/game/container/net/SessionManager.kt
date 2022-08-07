package play.example.game.container.net

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import io.netty.channel.Channel
import org.eclipse.collections.api.factory.Lists
import play.akka.AbstractTypedActor
import play.net.netty.getHostAndPort

class SessionManager(context: ActorContext<Command>) :
  AbstractTypedActor<SessionManager.Command>(context) {

  private var unhandledRequestReceivers = Lists.immutable.empty<ActorRef<UnhandledRequest>>()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::createSession)
      .accept(::registerUnhandledRequestReceiver)
      .acceptSignal<Terminated>(::onSessionClosed)
      .build()
  }

  private fun registerUnhandledRequestReceiver(cmd: RegisterUnhandledRequestReceiver) {
    unhandledRequestReceivers = unhandledRequestReceivers.newWith(cmd.receiver)
  }

  private fun createSession(cmd: CreateSession) {
    val hostAndPort = cmd.ch.remoteAddress().getHostAndPort()
    val session =
      context.spawn(SessionActor.create(cmd.ch, unhandledRequestReceivers.castToList()), hostAndPort.toString())
    sessionCount += 1
    context.watch(session)
  }

  private fun onSessionClosed() {
    sessionCount -= 1
  }

  companion object {
    @Volatile
    var sessionCount = 0
      private set

    fun create(): Behavior<Command> {
      return Behaviors.setup { ctx -> SessionManager(ctx) }
    }
  }

  interface Command
  data class CreateSession(val ch: Channel) : Command
  data class RegisterUnhandledRequestReceiver(val receiver: ActorRef<UnhandledRequest>) : Command
}
