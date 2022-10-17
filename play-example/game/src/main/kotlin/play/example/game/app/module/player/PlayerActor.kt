package play.example.game.app.module.player

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.example.game.app.module.player.event.*
import play.example.game.container.net.Session
import play.example.game.container.net.SessionActor
import play.example.module.login.message.LoginParams
import play.mvc.PlayerRequest
import play.mvc.Request
import play.mvc.Response

class PlayerActor(
  context: ActorContext<Command>,
  private val me: PlayerManager.Self,
  private val eventDispatcher: PlayerEventDispatcher,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler
) : AbstractTypedActor<PlayerActor.Command>(context) {

  private var session: Session? = null

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onEvent)
      .accept(::onNewDayStart)
      .accept(::onRequest)
      .accept(::onLogin)
      .accept(::onSessionClosed)
      .build()
  }

  private fun onSessionClosed(cmd: SessionClosed) {
    // TODO
    playerService.logout(me)
    eventDispatcher.dispatch(me, PlayerLogoutEvent(me.id))
  }

  private fun onLogin(cmd: Login) {
    val session = cmd.session
    this.session = session
    context.watchWith(session.actorRef, SessionClosed)
    eventDispatcher.dispatch(me, PlayerPreLoginEvent(me.id))
    val playerInfo = playerService.login(me, cmd.loginParams)
    session.tellActor(SessionActor.Subscribe(context.messageAdapter(Request::class.java) { RequestCommand(it) }))
    session.write(Response(cmd.request.header, 0, playerInfo))
    playerService.afterLogin(me)
  }

  private fun onRequest(cmd: RequestCommand) {
    val handlerActor = requestHandler.findHandlerActor(cmd.request)
    if (handlerActor != null) {
      handlerActor.tell(PlayerRequest(me.id, cmd.request))
    } else {
      requestHandler.handle(me, cmd.request)
    }
  }

  private fun onNewDayStart(cmd: PlayerManager.NewDayStart) {
    if (!playerService.isOnline(me.id)) {
      return
    }
    playerService.onNewDayStart(me)
  }

  private fun onEvent(event: PlayerEvent) {
    if (event is PlayerRequestEvent) {
      onRequest(event.message)
      return
    }
    eventDispatcher.dispatch(me, event)
  }

  companion object {
    fun create(
      self: PlayerManager.Self,
      eventDispatcher: PlayerEventDispatcher,
      playerService: PlayerService,
      requestHandler: PlayerRequestHandler
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        PlayerActor(ctx, self, eventDispatcher, playerService, requestHandler)
      }
    }
  }

  interface Command

  class Login(val request: Request, val loginParams: LoginParams, val session: Session) : Command

  class RequestCommand(val request: Request) : Command

  private object SessionClosed : Command
}
