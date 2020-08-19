package play.example.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.akka.sameBehavior
import play.akka.send
import play.akka.stoppedBehavior
import play.example.common.net.SessionActor
import play.example.common.net.message.WireMessage
import play.example.common.net.write
import play.example.module.account.message.LoginProto
import play.example.module.common.message.BoolValue
import play.example.module.player.PlayerManager
import play.example.module.player.controller.PlayerControllerInvoker
import play.mvc.Request
import play.mvc.Response

/**
 * Created by liang on 2020/6/27.
 */
class AccountActor(
  context: ActorContext<Command>,
  val id: Long,
  private val playerManager: ActorRef<PlayerManager.Command>
) : AbstractTypedActor<AccountActor.Command>(context) {

  private lateinit var loginParams: LoginProto
  private lateinit var session: ActorRef<SessionActor.Command>

  private val requestAdapter = context.messageAdapter(Request::class.java) {
    RequestAdapter(it)
  }

  private val waitingPlayerCreate = Behaviors.receive(Command::class.java)
    .onMessage(RequestAdapter::class.java, ::onRequest)
    .onSignal(Terminated::class.java, ::onSessionClosed)
    .build()


  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .apply(::onLogin)
      .build()
  }

  private fun onRequest(cmd: RequestAdapter): Behavior<Command> {
    val request = cmd.request
    return when (request.header.msgId.toInt()) {
      PlayerControllerInvoker.create -> {
        playerManager send PlayerManager.CreatePlayerRequest(id, request, loginParams, session)
        sameBehavior()
      }
      PlayerControllerInvoker.login -> {
        playerManager send PlayerManager.LoginPlayerRequest(id, request, loginParams, session)
        sameBehavior() // TODO stop?
      }
      else -> {
        context.log.warn("unhandled request: $request")
        sameBehavior()
      }
    }
  }

  private fun onLogin(cmd: Login): Behavior<Command> {
    loginParams = cmd.params
    this.session = cmd.session
    context.watch(session)
    session send SessionActor.Identify(id)
    session send SessionActor.Subscribe(requestAdapter)
    val hasPlayer = PlayerManager.isPlayerExists(id)
    session.write(Response(cmd.request.header, 0, WireMessage(BoolValue(hasPlayer))))
    return waitingPlayerCreate
  }

  private fun onSessionClosed(terminated: Terminated): Behavior<Command> = stoppedBehavior()

  companion object {
    fun create(id: Long, playerManager: ActorRef<PlayerManager.Command>): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountActor(ctx, id, playerManager)
      }
    }
  }

  interface Command

  class Login(val request: Request, val params: LoginProto, val session: ActorRef<SessionActor.Command>) : Command

  private class RequestAdapter(val request: Request) : Command
}
