package play.example.common.net

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.google.common.collect.ImmutableList
import io.netty.channel.Channel
import play.akka.AbstractTypedActor
import play.net.netty.getHostAndPort

class SessionManager(context: ActorContext<Command>) :
  AbstractTypedActor<SessionManager.Command>(context) {

  init {
    subscribe<RegisterUnhandledRequestReceiver>()
  }

  private var unhandledRequestReceivers = emptyList<ActorRef<UnhandledRequest>>()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::createSession)
      .accept(::registerUnhandledRequestReceiver)
      .onSignal<Terminated>(::onSessionClosed)
      .build()
  }

  private fun registerUnhandledRequestReceiver(cmd: RegisterUnhandledRequestReceiver) {
    unhandledRequestReceivers = ImmutableList
      .builder<ActorRef<UnhandledRequest>>()
      .addAll(unhandledRequestReceivers)
      .add(cmd.receiver)
      .build()
  }

  private fun createSession(cmd: CreateSession) {
    val hostAndPort = cmd.ch.remoteAddress().getHostAndPort()
    val session =
      context.spawn(SessionActor.create(cmd.ch, unhandledRequestReceivers), hostAndPort.toString())
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
