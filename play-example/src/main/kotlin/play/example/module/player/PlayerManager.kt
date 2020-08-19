package play.example.module.player

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.akka.resumeSupervisor
import play.akka.send
import play.db.QueryService
import play.example.common.id.GameUIDGenerator
import play.example.common.net.SessionActor
import play.example.common.net.write
import play.example.common.scheduling.Cron
import play.example.module.account.message.LoginProto
import play.example.module.player.domain.PlayerErrorCode
import play.example.module.player.entity.PlayerInfo
import play.example.module.player.event.PlayerEvent
import play.example.module.player.event.PlayerEventDispatcher
import play.example.module.player.exception.PlayerNotExistsException
import play.mvc.Request
import play.mvc.Response
import play.util.concurrent.awaitSuccessOrThrow
import play.util.scheduling.Scheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class PlayerManager(
  context: ActorContext<Command>,
  private val eventDispatcher: PlayerEventDispatcher,
  private val queryService: QueryService,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler,
  scheduler: Scheduler
) :
  AbstractTypedActor<PlayerManager.Command>(context) {

  init {
    queryService.foreach(PlayerInfo::class.java, listOf("name")) { result ->
      val id = result.getLong("id")
      val name = result.getString("name")
      add(id, name)
    }.awaitSuccessOrThrow(5000)

    scheduler.scheduleCron(Cron.EveryDay) { self send NewDayStart }
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
      session write Response(cmd.request.header, PlayerErrorCode.PlayerNotExists)
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
    if (!isPlayerNameAvailable(playerId, playerName)) {
      cmd.session write Response(cmd.request.header, PlayerErrorCode.PlayerNameNotAvailable)
      return
    }
    playerService.createPlayer(playerId, playerName)
    add(playerId, playerName)
    cmd.session write Response(cmd.request.header, 0)
  }

  private fun onEvent(event: PlayerEvent) {
    getPlayer(event.playerId)?.tell(event)
  }

  private fun getPlayer(id: Long): ActorRef<PlayerActor.Command>? {
    if (!isPlayerExists(id)) {
      return null
    }
    val actorName = id.toString()
    val opt = context.getChild(actorName)
    if (opt.isPresent) {
      return opt.get().unsafeUpcast()
    }
    return context.spawn(
      resumeSupervisor(
        PlayerActor.create(id, eventDispatcher, playerService, requestHandler)
      ), actorName
    )
  }

  companion object {
    private val idToName: ConcurrentMap<Long, String> = ConcurrentHashMap(1024)
    private val serverToNameToId: ConcurrentMap<Int, ConcurrentMap<String, Long>> = ConcurrentHashMap(1024)

    private fun add(playerId: Long, playerName: String) {
      val prevName = idToName.putIfAbsent(playerId, playerName)
      assert(prevName != null)
      val serverId = GameUIDGenerator.getServerId(playerId).toInt()
      val nameToId = serverToNameToId.computeIfAbsent(serverId) { ConcurrentHashMap(1024) }
      val prevId = nameToId.putIfAbsent(playerName, playerId)
      assert(prevId != null)
    }

    private fun isPlayerNameAvailable(id: Long, name: String): Boolean {
      val serverId = GameUIDGenerator.getServerId(id).toInt()
      return serverToNameToId[serverId]?.containsKey(name) ?: true
    }

    fun isPlayerExists(playerId: Long): Boolean {
      return idToName.containsKey(playerId)
    }

    fun create(
      eventDispatcher: PlayerEventDispatcher,
      queryService: QueryService,
      playerService: PlayerService,
      requestHandler: PlayerRequestHandler,
      scheduler: Scheduler
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        PlayerManager(ctx, eventDispatcher, queryService, playerService, requestHandler, scheduler)
      }
    }

    fun getPlayerNameOrThrow(playerId: Long): String {
      return idToName[playerId] ?: throw PlayerNotExistsException(playerId)
    }

    fun getPlayerNameOrEmpty(playerId: Long): String {
      return idToName[playerId] ?: ""
    }

  }

  interface Command

  data class PlayerMessage(val playerId: Long, val message: PlayerActor.Command) : Command

  class CreatePlayerRequest(
    val id: Long,
    val request: Request,
    val loginParams: LoginProto,
    val session: ActorRef<SessionActor.Command>
  ) : Command

  class LoginPlayerRequest(
    val id: Long,
    val request: Request,
    val loginParams: LoginProto,
    val session: ActorRef<SessionActor.Command>
  ) : Command

  object NewDayStart : Command, PlayerActor.Command
}
