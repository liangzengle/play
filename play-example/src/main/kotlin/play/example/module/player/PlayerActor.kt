package play.example.module.player

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
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
import play.example.module.player.scheduling.PlayerRepeatedSchedule
import play.example.module.player.scheduling.PlayerSchedule
import play.example.module.player.scheduling.PlayerScheduleCancellation
import play.mvc.Request
import play.mvc.Response
import play.util.time.currentMillis
import java.time.Duration

class PlayerActor(
  context: ActorContext<Command>,
  private val timer: TimerScheduler<Command>,
  playerId: Long,
  private val eventDispatcher: PlayerEventDispatcher,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler
) : AbstractTypedActor<PlayerActor.Command>(context) {

  private val me = Self(playerId)

  private var session: ActorRef<SessionActor.Command>? = null

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onSchedule)
      .accept(::onRepeatedSchedule)
      .accept(::onScheduleCancel)
      .accept(::onEvent)
      .accept(::onNewDayStart)
      .accept(::onRequest)
      .accept(::onLogin)
      .build()
  }

  private fun onScheduleCancel(cancellation: PlayerScheduleCancellation) {
    timer.cancel(cancellation.triggerEvent)
  }

  private fun onRepeatedSchedule(schedule: PlayerRepeatedSchedule) {
    if (schedule.triggerImmediately) {
      onEvent(schedule.triggerEvent)
    }
    timer.startTimerWithFixedDelay(schedule.triggerEvent, schedule.interval)
  }

  private fun onSchedule(schedule: PlayerSchedule) {
    timer.startSingleTimer(schedule.triggerEvent, Duration.ofMillis(schedule.triggerTimeMillis - currentMillis()))
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
        Behaviors.withTimers { timer ->
          PlayerActor(ctx, timer, id, eventDispatcher, playerService, requestHandler)
        }
      }
    }
  }

  interface Command

  class Login(val request: Request, val loginParams: LoginProto, val session: ActorRef<SessionActor.Command>) : Command

  class RequestCommand(val request: Request) : Command
}
