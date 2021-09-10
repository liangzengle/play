package play.example.game.app.module.player

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import play.akka.AbstractTypedActor
import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.player.event.*
import play.example.game.app.module.task.TaskEventReceiver
import play.example.game.container.net.Session
import play.example.game.container.net.SessionActor
import play.mvc.PlayerRequest
import play.mvc.Request
import play.mvc.Response

class PlayerActor(
  context: ActorContext<Command>,
  private val timer: TimerScheduler<Command>,
  playerId: Long,
  private val eventDispatcher: PlayerEventDispatcher,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler,
  private val taskEventReceiver: TaskEventReceiver
) : AbstractTypedActor<PlayerActor.Command>(context) {

  private val me = Self(playerId)

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
    eventDispatcher.dispatch(me, PlayerLoginEvent(me.id))
  }

  private fun onLogin(cmd: Login) {
    val session = cmd.session
    this.session = session
    context.watchWith(session.actorRef, SessionClosed)
    eventDispatcher.dispatch(me, PlayerPreLoginEvent(me.id))
    val playerInfo = playerService.login(me, cmd.loginParams)
    session.tellActor(SessionActor.Subscribe(context.messageAdapter(Request::class.java) { RequestCommand(it) }))
    session.write(Response(cmd.request.header, 0, playerInfo))
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
    if (event is PlayerTaskEvent) {
      taskEventReceiver.receive(me, event.taskEvent)
      return
    }
    if (event is PromisedPlayerEvent<*>) {
      eventDispatcher.dispatchPromised(me, event)
    } else {
      eventDispatcher.dispatch(me, event)
    }
  }

  companion object {
    fun create(
      id: Long,
      eventDispatcher: PlayerEventDispatcher,
      playerService: PlayerService,
      requestHandler: PlayerRequestHandler,
      taskEventReceiver: TaskEventReceiver
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        Behaviors.withTimers { timer ->
          PlayerActor(ctx, timer, id, eventDispatcher, playerService, requestHandler, taskEventReceiver)
        }
      }
    }
  }

  interface Command

  class Login(val request: Request, val loginParams: LoginParams, val session: Session) : Command

  class RequestCommand(val request: Request) : Command

  private object SessionClosed : Command
}
