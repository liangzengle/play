package play.example.module.player

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.akka.send
import play.example.common.net.SessionActor
import play.example.common.net.message.WireMessage
import play.example.common.net.write
import play.example.module.account.message.LoginProto
import play.example.module.player.event.PlayerEvent
import play.example.module.player.event.PlayerEventDispatcher
import play.example.module.player.event.PlayerPreLoginEvent
import play.example.module.player.event.PlayerRequestEvent
import play.mvc.Request
import play.mvc.Response

class PlayerActor(
  context: ActorContext<Command>,
  playerId: Long,
  private val eventDispatcher: PlayerEventDispatcher,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler
) : AbstractTypedActor<PlayerActor.Command>(context) {

  private val me = Self(playerId)

  private var session: ActorRef<SessionActor.Command>? = null

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onEvent)
      .accept(::onEvent)
      .accept(::onNewDayStart)
      .accept(::onRequest)
      .accept(::onLogin)
      .build()
  }

  private fun onLogin(cmd: Login) {
    val session = cmd.session
    this.session = session
    eventDispatcher.dispatch(me, PlayerPreLoginEvent(me.id))
    val playerProto = playerService.login(me, cmd.loginParams)
    session send SessionActor.Subscribe(context.messageAdapter(Request::class.java) { RequestCommand(it) })
    session.write(Response(cmd.request.header, 0, WireMessage(playerProto)))
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
      id: Long,
      eventDispatcher: PlayerEventDispatcher,
      playerService: PlayerService,
      requestHandler: PlayerRequestHandler
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        PlayerActor(ctx, id, eventDispatcher, playerService, requestHandler)
      }
    }
  }

  interface Command

  class Login(val request: Request, val loginParams: LoginProto, val session: ActorRef<SessionActor.Command>) : Command


  class RequestCommand(val request: Request) : Command
}
