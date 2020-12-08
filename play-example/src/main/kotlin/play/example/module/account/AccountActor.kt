package play.example.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.Log
import play.akka.*
import play.example.common.net.SessionActor
import play.example.common.net.write
import play.example.module.StatusCode
import play.example.module.account.entity.AccountEntityCache
import play.example.module.account.message.LoginParams
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
  private val accountEntityCache: AccountEntityCache,
  private val playerManager: ActorRef<PlayerManager.Command>
) : AbstractTypedActor<AccountActor.Command>(context) {

  private lateinit var loginParams: LoginParams
  private lateinit var session: ActorRef<SessionActor.Command>

  private val requestAdapter = context.messageAdapter(Request::class.java) {
    RequestCommand(it)
  }

  private val waitingPlayerCreate: Behavior<Command> = newBehaviorBuilder()
    .accept(::onRequest)
    .acceptSignal(::onSessionClosed)
    .build()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onLogin)
      .build()
  }

  private fun onRequest(cmd: RequestCommand): Behavior<Command> {
    val request = cmd.request
    return when (request.header.msgId.toInt()) {
      PlayerControllerInvoker.create -> {
        val account = accountEntityCache.getOrNull(id)
        // Account may create failed, make sure it has been created.
        if (account == null) {
          Log.error("Player($id)创建失: Account未创建")
          session.write(Response(request.header, StatusCode.Failure.getErrorCode()))
        } else {
          playerManager send PlayerManager.CreatePlayerRequest(id, request, loginParams, session)
        }
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
    session.write(Response(cmd.request.header, 0, hasPlayer))
    return waitingPlayerCreate
  }

  private fun onSessionClosed(terminated: Terminated): Behavior<Command> = stoppedBehavior()

  companion object {
    fun create(
      id: Long,
      accountEntityCache: AccountEntityCache,
      playerManager: ActorRef<PlayerManager.Command>
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountActor(ctx, id, accountEntityCache, playerManager)
      }
    }
  }

  interface Command

  class Login(val request: Request, val params: LoginParams, val session: ActorRef<SessionActor.Command>) : Command

  private class RequestCommand(val request: Request) : Command
}
