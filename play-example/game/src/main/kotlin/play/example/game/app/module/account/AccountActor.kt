package play.example.game.app.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.Log
import play.akka.*
import play.example.common.StatusCode
import play.example.game.app.module.account.entity.AccountEntityCache
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerModule
import play.example.game.app.module.player.PlayerService
import play.example.game.container.net.NetModule
import play.example.game.container.net.Session
import play.example.game.container.net.SessionActor
import play.example.game.container.net.SessionCloseReason
import play.example.module.login.message.LoginParams
import play.mvc.Request
import play.mvc.Response

/**
 * Created by liang on 2020/6/27.
 */
class AccountActor(
  context: ActorContext<Command>,
  val id: Long,
  private val accountEntityCache: AccountEntityCache,
  private val playerManager: ActorRef<PlayerManager.Command>,
  private val playerService: PlayerService
) : AbstractTypedActor<AccountActor.Command>(context) {

  private var loginParams: LoginParams? = null
  private var session: Session? = null

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
    val session = checkNotNull(this.session)
    val loginParams = checkNotNull(this.loginParams)
    val request = cmd.request
    return when (request.header.msgId.toInt()) {
      PlayerModule.create -> {
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

      PlayerModule.login -> {
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
    val preSession = this.session
    preSession?.tellActor(
      SessionActor.NotifyAndClose(
        "Repeat login",
        NetModule.sessionClosePush(SessionCloseReason.RepeatLogin)
      )
    )

    val newSession = cmd.session
    context.watch(newSession.actorRef)
    newSession.tellActor(SessionActor.BindId(id))
    newSession.tellActor(SessionActor.Subscribe(requestAdapter))
    val hasPlayer = playerService.isPlayerExists(id)
    newSession.write(Response(cmd.request.header, 0, hasPlayer))
    this.loginParams = cmd.params
    this.session = newSession
    return waitingPlayerCreate
  }

  private fun onSessionClosed(terminated: Terminated): Behavior<Command> = stoppedBehavior()

  companion object {
    fun create(
      id: Long,
      accountEntityCache: AccountEntityCache,
      playerManager: ActorRef<PlayerManager.Command>,
      playerService: PlayerService
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountActor(ctx, id, accountEntityCache, playerManager, playerService)
      }
    }
  }

  interface Command

  class Login(val request: Request, val params: LoginParams, val session: Session) : Command

  private class RequestCommand(val request: Request) : Command
}
