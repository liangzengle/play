package play.example.game.app.module.player

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.akka.resumeSupervisor
import play.akka.send
import play.example.common.akka.scheduling.ActorCronScheduler
import play.example.common.scheduling.Cron
import play.example.game.app.module.account.message.LoginParams
import play.example.game.app.module.player.domain.PlayerErrorCode
import play.example.game.app.module.player.event.PlayerEvent
import play.example.game.app.module.player.event.PlayerEventDispatcher
import play.example.game.app.module.player.event.PromisedPlayerEvent
import play.example.game.app.module.task.TaskEventReceiver
import play.example.game.container.net.SessionActor
import play.example.game.container.net.write
import play.mvc.Request
import play.mvc.Response
import play.scheduling.Scheduler
import play.util.exception.NoStackTraceException
import play.util.unsafeCast

class PlayerManager(
  context: ActorContext<Command>,
  private val eventDispatcher: PlayerEventDispatcher,
  private val playerIdNameCache: PlayerIdNameCache,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler,
  cronScheduler: ActorCronScheduler<Command>,
  private val taskEventReceiver: TaskEventReceiver
) : AbstractTypedActor<PlayerManager.Command>(context) {

  init {
    cronScheduler.schedule(Cron.EveryDay, NewDayStart)
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onEvent)
      .accept(::createPlayer)
      .accept(::onNewDayStart)
      .accept(::onPlayerLogin)
      .build()
  }

  private fun onPlayerLogin(cmd: LoginPlayerRequest) {
    val playerId = cmd.id
    val session = cmd.session
    if (!playerService.isPlayerExists(playerId)) {
      session write Response(cmd.request.header, PlayerErrorCode.PlayerNotExists.getErrorCode())
      return
    }
    val player = getPlayer(playerId)!!
    player send PlayerActor.Login(cmd.request, cmd.loginParams, session)
  }

  private fun onNewDayStart(cmd: NewDayStart) {
    sendToChildren(cmd)
  }

  private fun sendToChildren(message: PlayerActor.Command) {
    context.children.forEach { it.unsafeUpcast<PlayerActor.Command>() send message }
  }

  private fun createPlayer(cmd: CreatePlayerRequest) {
    val playerId = cmd.id
    val playerName = cmd.request.body.readString()
    if (!playerIdNameCache.isPlayerNameAvailable(playerId, playerName)) {
      cmd.session write Response(cmd.request.header, PlayerErrorCode.PlayerNameNotAvailable.getErrorCode())
      return
    }
    playerService.createPlayer(playerId, playerName)
    playerIdNameCache.add(playerId, playerName)
    cmd.session write Response(cmd.request.header, 0)
  }

  private fun onEvent(event: PlayerEvent) {
    val playerId = event.playerId
    val player = getPlayer(playerId)
    if (player == null) {
      log.error("$event not delivered: Player($playerId) not exists.")
      if (event is PromisedPlayerEvent<*>) {
        event.promise.failure(NoStackTraceException("Player($playerId) not exists."))
      }
      return
    }
    player.tell(event)
  }

  private fun getPlayer(id: Long): ActorRef<PlayerActor.Command>? {
    if (!playerIdNameCache.isPlayerExists(id)) {
      return null
    }
    val actorName = id.toString()
    val opt = context.getChild(actorName)
    if (opt.isPresent) {
      return opt.get().unsafeCast()
    }
    return context.spawn(
      resumeSupervisor(
        PlayerActor.create(id, eventDispatcher, playerService, requestHandler, taskEventReceiver)
      ),
      actorName
    )
  }

  companion object {
    fun create(
      eventDispatcher: PlayerEventDispatcher,
      playerIdNameCache: PlayerIdNameCache,
      playerService: PlayerService,
      requestHandler: PlayerRequestHandler,
      scheduler: Scheduler,
      taskEventReceiver: TaskEventReceiver
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        PlayerManager(
          ctx,
          eventDispatcher,
          playerIdNameCache,
          playerService,
          requestHandler,
          ActorCronScheduler(scheduler, ctx),
          taskEventReceiver
        )
      }
    }
  }

  interface Command

  data class PlayerMessage(val playerId: Long, val message: PlayerActor.Command) : Command

  class CreatePlayerRequest(
    val id: Long,
    val request: Request,
    val loginParams: LoginParams,
    val session: ActorRef<SessionActor.Command>
  ) : Command

  class LoginPlayerRequest(
    val id: Long,
    val request: Request,
    val loginParams: LoginParams,
    val session: ActorRef<SessionActor.Command>
  ) : Command

  object NewDayStart : Command, PlayerActor.Command
}
