package play.example.game.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import java.util.*
import kotlin.time.seconds
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntLongMaps
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import play.Log
import play.akka.AbstractTypedActor
import play.akka.send
import play.db.QueryService
import play.example.common.id.GameUIDGenerator
import play.example.game.base.net.SessionActor
import play.example.game.base.net.SessionManager
import play.example.game.base.net.UnhandledRequest
import play.example.game.base.net.write
import play.example.game.module.account.controller.AccountControllerInvoker
import play.example.game.module.account.domain.AccountErrorCode
import play.example.game.module.account.domain.AccountId
import play.example.game.module.account.entity.Account
import play.example.game.module.account.entity.AccountEntityCache
import play.example.game.module.account.message.LoginParams
import play.example.game.module.platform.PlatformServiceProvider
import play.example.game.module.platform.domain.Platform
import play.example.game.module.player.PlayerEntityCacheInitializer
import play.example.game.module.player.PlayerManager
import play.example.game.module.server.ServerService
import play.mvc.Request
import play.mvc.Response
import play.util.collection.ConcurrentObjectLongMap
import play.util.control.Result2
import play.util.control.err
import play.util.control.getCause
import play.util.control.ok
import play.util.max
import play.util.primitive.high16
import play.util.primitive.low16
import play.util.primitive.toInt
import play.util.unsafeCast

class AccountManager(
  context: ActorContext<Command>,
  private val platformServiceProvider: PlatformServiceProvider,
  queryService: QueryService,
  sessionManager: ActorRef<SessionManager.Command>,
  private val serverService: ServerService,
  private val accountCache: AccountEntityCache,
  private val playerManager: ActorRef<PlayerManager.Command>,
  private val playerEntityCacheInitializer: PlayerEntityCacheInitializer
) : AbstractTypedActor<AccountManager.Command>(context) {

  private val idGenerators: MutableIntObjectMap<GameUIDGenerator>

  init {
    accountIdToId = queryService.fold(Account::class.java, ConcurrentObjectLongMap<AccountId>()) { map, account ->
      val platformService = platformServiceProvider.getService(account.platformId.toInt())
      val accountId = platformService.toAccountId(account)
      map[accountId] = account.id
      map
    }.get(5.seconds)

    val maxIds = IntLongMaps.mutable.empty()
    for ((accountId, id) in accountIdToId) {
      val key = toInt(accountId.platformId.toShort(), accountId.serverId)
      maxIds.updateValue(key, id) { it max id }
    }

    val idGeneratorMap = IntObjectHashMap<GameUIDGenerator>(maxIds.keyValuesView().size())
    for (e in maxIds.keyValuesView()) {
      val key = e.one
      val value = GameUIDGenerator(e.one.high16().toInt(), e.one.low16().toInt(), OptionalLong.of(e.two))
      idGeneratorMap.put(key, value)
    }
    idGenerators = idGeneratorMap

    val subscriber =
      context.messageAdapter(UnhandledRequest::class.java) { RequestCommand(it.request, it.session) }
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

  private fun onRequest(cmd: RequestCommand) {
    val request = cmd.request
    val session = cmd.session
    when (request.header.msgId.toInt()) {
      AccountControllerInvoker.login -> login(request, session)
      AccountControllerInvoker.ping -> {
        println("ping")
        session write Response(request.header, 0, "hello")
      }
      else -> Log.warn { "unhandled request: $request" }
    }
  }

  private fun login(request: Request, session: ActorRef<SessionActor.Command>) {
    val requestBody = request.body
    val params = ProtoBuf.decodeFromByteArray<LoginParams>(requestBody.bytes)
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
      context.spawn(AccountActor.create(accountId, accountCache, playerManager), accountActorName)
    }
    accountActor send AccountActor.Login(request, params, session)
  }

  private fun getOrCreateAccount(params: LoginParams): Result2<Long> {
    val platformName = params.platform
    val platform = Platform.getOrNull(platformName)
    val serverId = params.serverId.toShort()
    if (serverId.toInt() != params.serverId) {
      return AccountErrorCode.ParamErr
    }
    if (platform == null) {
      return AccountErrorCode.NoSuchPlatform
    }
    if (!serverService.isPlatformValid(platform)) {
      return AccountErrorCode.InvalidPlatform
    }
    if (!serverService.isServerIdValid(serverId.toInt())) {
      return AccountErrorCode.InvalidServerId
    }
    val platformService = platformServiceProvider.getServiceOrNull(platform) ?: return AccountErrorCode.Failure
    val paramValidateStatus = platformService.validateLoginParams(params)
    if (paramValidateStatus != 0) {
      return err(paramValidateStatus)
    }
    val accountId = platformService.getAccountId(params)
    val id = accountIdToId[accountId]
    if (id != null) {
      return ok(id)
    }
    // 账号不存在，新注册用户
    val maybeId = nextId(platform.getId(), serverId)
    if (maybeId.isEmpty) {
      return AccountErrorCode.IdExhausted
    }
    val account = platformService.newAccount(maybeId.asLong, platform.getId(), serverId, params.account, params)
    accountIdToId[accountId] = account.id
    createAsync(account)
    return ok(maybeId.asLong)
  }

  private fun createAsync(account: Account) {
    accountCache.unsafeOps().initWithEmptyValue(account.id)
    accountCache.create(account)
    Log.info { "账号创建成功: $account" }
    val id = account.id
    future {
      playerEntityCacheInitializer.initWithEmptyValue(id)
    }.onComplete {
      if (it.isSuccess) {
        Log.info { "玩家实体缓存初始化完成: $id" }
      } else {
        Log.error(it.getCause()) { "玩家实体缓存初始化失败: $id" }
      }
    }
  }

  companion object {
    private lateinit var accountIdToId: ConcurrentObjectLongMap<AccountId>

    fun create(
      platformServiceProvider: PlatformServiceProvider,
      queryService: QueryService,
      sessionManager: ActorRef<SessionManager.Command>,
      serverService: ServerService,
      accountCache: AccountEntityCache,
      playerManager: ActorRef<PlayerManager.Command>,
      playerEntityCacheInitializer: PlayerEntityCacheInitializer
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountManager(
          ctx,
          platformServiceProvider,
          queryService,
          sessionManager,
          serverService,
          accountCache,
          playerManager,
          playerEntityCacheInitializer
        )
      }
    }
  }

  interface Command
  private class RequestCommand(val request: Request, val session: ActorRef<SessionActor.Command>) : Command
}
