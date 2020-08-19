package play.example.common.net

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import io.netty.channel.Channel
import play.akka.AbstractTypedActor
import play.net.netty.toHostAndPort

class SessionManager(context: ActorContext<Command>) :
  AbstractTypedActor<SessionManager.Command>(context) {

  init {
    context.system.eventStream()
      .tell(EventStream.Subscribe(RegisterUnhandledRequestReceiver::class.java, self.unsafeUpcast()))
  }

  private var unhandledRequestReceivers = ArrayList<ActorRef<UnhandledRequest>>(4)

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::createSession)
      .accept(::registerUnhandledRequestReceiver)
      .onSignal<Terminated>(::onSessionClosed)
      .build()
  }

  private fun registerUnhandledRequestReceiver(cmd: RegisterUnhandledRequestReceiver) {
    unhandledRequestReceivers.add(cmd.receiver)
  }

  private fun createSession(cmd: CreateSession) {
    val hostAndPort = cmd.ch.remoteAddress().toHostAndPort()
    val receivers: List<ActorRef<UnhandledRequest>> = unhandledRequestReceivers // expose as immutable
    val session =
      context.spawn(SessionActor.create(cmd.ch, receivers), hostAndPort.toString())
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
