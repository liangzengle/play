package play.example.module.player

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import kotlin.time.seconds
import play.akka.AbstractTypedActor
import play.akka.resumeSupervisor
import play.akka.send
import play.db.QueryService
import play.example.common.akka.scheduling.ActorCronScheduler
import play.example.common.id.GameUIDGenerator
import play.example.common.net.SessionActor
import play.example.common.net.write
import play.example.common.scheduling.Cron
import play.example.module.account.message.LoginParams
import play.example.module.player.domain.PlayerErrorCode
import play.example.module.player.entity.Player2
import play.example.module.player.event.PlayerEvent
import play.example.module.player.event.PlayerEventDispatcher
import play.example.module.player.exception.PlayerNotExistsException
import play.example.module.task.TaskEventReceiver
import play.mvc.Request
import play.mvc.Response
import play.util.collection.ConcurrentIntObjectMap
import play.util.collection.ConcurrentLongObjectMap
import play.util.collection.ConcurrentObjectLongMap
import play.util.scheduling.Scheduler
import play.util.unsafeCast

class PlayerManager(
  context: ActorContext<Command>,
  private val eventDispatcher: PlayerEventDispatcher,
  queryService: QueryService,
  private val playerService: PlayerService,
  private val requestHandler: PlayerRequestHandler,
  cronScheduler: ActorCronScheduler<Command>,
  private val taskEventReceiver: TaskEventReceiver
) : AbstractTypedActor<PlayerManager.Command>(context) {

  init {
    val idToName = ConcurrentLongObjectMap<String>()
    val serverToNameToId = ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>()
    queryService.foreach(Player2::class.java, listOf("name")) { result ->
      val id = result.getLong("id")
      val name = result.getString("name")
      add(id, name, idToName, serverToNameToId)
    }.await(5.seconds)
    PlayerManager.idToName = idToName
    PlayerManager.serverToNameToId = serverToNameToId

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
    if (!isPlayerNameAvailable(playerId, playerName)) {
      cmd.session write Response(cmd.request.header, PlayerErrorCode.PlayerNameNotAvailable.getErrorCode())
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
    private lateinit var idToName: ConcurrentLongObjectMap<String>
    private lateinit var serverToNameToId: ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>

    private fun add(playerId: Long, playerName: String) {
      add(playerId, playerName, idToName, serverToNameToId)
    }

    private fun add(
      playerId: Long,
      playerName: String,
      idToName: ConcurrentLongObjectMap<String>,
      serverToNameToId: ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>
    ) {
      val prevName = idToName.putIfAbsent(playerId, playerName)
      assert(prevName == null)
      val serverId = GameUIDGenerator.getServerId(playerId).toInt()
      val nameToId = serverToNameToId.computeIfAbsent(serverId) { ConcurrentObjectLongMap(1024) }
      val prevId = nameToId.putIfAbsent(playerName, playerId)
      assert(prevId == null)
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
      scheduler: Scheduler,
      taskEventReceiver: TaskEventReceiver
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        PlayerManager(
          ctx,
          eventDispatcher,
          queryService,
          playerService,
          requestHandler,
          ActorCronScheduler(scheduler, ctx),
          taskEventReceiver
        )
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
