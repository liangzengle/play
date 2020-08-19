package play.example.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntLongMaps
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.Log
import play.akka.AbstractTypedActor
import play.akka.send
import play.db.QueryService
import play.example.common.id.GameUIDGenerator
import play.example.common.net.SessionActor
import play.example.common.net.SessionManager
import play.example.common.net.UnhandledRequest
import play.example.common.net.message.WireMessage
import play.example.common.net.write
import play.example.module.account.controller.AccountControllerInvoker
import play.example.module.account.domain.AccountErrorCode
import play.example.module.account.domain.AccountId
import play.example.module.account.entity.Account
import play.example.module.account.entity.AccountCache
import play.example.module.account.message.LoginProto
import play.example.module.account.message.PongProto
import play.example.module.platform.PlatformServiceProvider
import play.example.module.platform.domain.Platform
import play.example.module.player.PlayerManager
import play.example.module.server.ServerService
import play.mvc.Request
import play.mvc.Response
import play.util.concurrent.awaitSuccessOrThrow
import play.util.control.Result2
import play.util.control.err
import play.util.control.ok
import play.util.max
import play.util.primitive.high16
import play.util.primitive.low16
import play.util.primitive.toInt
import play.util.unsafeCast
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class AccountManager(
  context: ActorContext<Command>,
  private val platformServiceProvider: PlatformServiceProvider,
  queryService: QueryService,
  sessionManager: ActorRef<SessionManager.Command>,
  private val serverService: ServerService,
  private val accountCache: AccountCache,
  private val playerManager: ActorRef<PlayerManager.Command>
) : AbstractTypedActor<AccountManager.Command>(context) {

  private val idGenerators: MutableIntObjectMap<GameUIDGenerator>

  init {
    accountIdToId = queryService.fold(Account::class.java, ConcurrentHashMap<AccountId, Long>()) { map, account ->
      val platformService = platformServiceProvider.getService(account.platformId.toInt())
      val accountId = platformService.toAccountId(account)
      map[accountId] = account.id
      map
    }.awaitSuccessOrThrow(5000)

    val maxIds = IntLongMaps.mutable.empty()
    for ((accountId, id) in accountIdToId) {
      val key = toInt(accountId.platformId.toShort(), accountId.serverId)
      maxIds.updateValue(key, id) { it max id }
    }

    idGenerators = IntObjectMaps.mutable.from(
      maxIds.keyValuesView(),
      { it.one },
      { GameUIDGenerator(it.one.high16().toInt(), it.one.low16().toInt(), OptionalLong.of(it.two)) }
    )

    val subscriber =
      context.messageAdapter(UnhandledRequest::class.java) { RequestBeforeLogin(it.request, it.session) }
    sessionManager.tell(SessionManager.RegisterUnhandledRequestReceiver(subscriber))
  }

  private fun nextId(platformId: Byte, serverId: Short): OptionalLong {
    val key = toInt(platformId.toShort(), serverId)
    val idGenerator = idGenerators.getIfAbsentPut(key) {
      GameUIDGenerator(
        platformId.toInt(),
        serverId.toInt(),
        OptionalLong.empty()
      )
    }
    return idGenerator.next()
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onRequest)
      .build()
  }

  private fun onRequest(cmd: RequestBeforeLogin) {
    val request = cmd.request
    val session = cmd.session
    when (request.header.msgId.toInt()) {
      AccountControllerInvoker.login -> login(request, session)
      AccountControllerInvoker.ping -> {
        println("ping")
        session write Response(request.header, 0, WireMessage(PongProto("hello")))
      }
      else -> Log.warn { "unhandled request: $request" }
    }
  }

  private fun login(request: Request, session: ActorRef<SessionActor.Command>) {
    val params = LoginProto.ADAPTER.decode(request.body.readByteArray())
    val result = getOrCreateAccount(params)
    if (result.isErr()) {
      session write Response(request.header, result.getErrorCode())
      Log.error { "登录失败: errorCode=${result.getErrorCode()} params=$params" }
      return
    }
    val accountId = result.get()
    val accountActorName = accountId.toString()
    val maybeAccountActor = context.getChild(accountActorName)
    val accountActor = if (maybeAccountActor.isPresent) {
      maybeAccountActor.unsafeCast()
    } else {
      context.spawn(AccountActor.create(accountId, playerManager), accountActorName)
    }
    accountActor send AccountActor.Login(request, params, session)
  }

  @Suppress("FoldInitializerAndIfToElvis")
  private fun getOrCreateAccount(params: LoginProto): Result2<Long> {
    val platformName = params.platform
    val platform = Platform.getOrNull(platformName)
    val serverId = params.serverId.toShort()
    if (serverId.toInt() != params.serverId) {
      return err(AccountErrorCode.ParamErr)
    }
    if (platform == null) {
      return err(AccountErrorCode.NoSuchPlatform)
    }
    if (!serverService.isPlatformNameValid(platform)) {
      return err(AccountErrorCode.InvalidPlatform)
    }
    if (!serverService.isServerIdValid(serverId.toInt())) {
      return err(AccountErrorCode.InvalidServerId)
    }
    val platformService = platformServiceProvider.getServiceOrNull(platformName)
    if (platformService == null) {
      return err(AccountErrorCode.Failure)
    }
    val accountId = platformService.getAccountId(params)
    val id = accountIdToId[accountId]
    if (id != null) {
      return ok(id)
    }
    val maybeId = nextId(platform.getId(), serverId)
    if (maybeId.isEmpty) {
      return err(AccountErrorCode.IdExhausted)
    }
    val account = platformService.createAccount(maybeId.asLong, platform.getId(), serverId, params.account, params)
    accountCache.create(account)
    // TODO log account create
    return ok(maybeId.asLong)
  }

  companion object {
    private lateinit var accountIdToId: ConcurrentMap<AccountId, Long>

    private lateinit var idToAccountId: ConcurrentMap<Long, AccountId>

    fun create(
      platformServiceProvider: PlatformServiceProvider,
      queryService: QueryService,
      sessionManager: ActorRef<SessionManager.Command>,
      serverService: ServerService,
      accountCache: AccountCache,
      playerManager: ActorRef<PlayerManager.Command>
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountManager(
          ctx,
          platformServiceProvider,
          queryService,
          sessionManager,
          serverService,
          accountCache,
          playerManager
        )
      }
    }
  }

  interface Command

  private data class RequestBeforeLogin(val request: Request, val session: ActorRef<SessionActor.Command>) : Command

  private data class AccountLogin(val platformName: String, val serverId: Int, val name: String) : Command
}
