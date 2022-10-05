package play.example.game.app.module.account

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.Log
import play.akka.AbstractTypedActor
import play.akka.logging.ActorMDC
import play.codec.MessageCodec
import play.example.game.app.module.account.domain.AccountErrorCode
import play.example.game.app.module.account.entity.Account
import play.example.game.app.module.account.entity.AccountEntityCache
import play.example.game.app.module.platform.PlatformServiceProvider
import play.example.game.app.module.platform.domain.Platforms
import play.example.game.app.module.player.PlayerEntityCacheInitializer
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.server.ServerService
import play.example.game.container.login.LoginDispatcherActor
import play.example.game.container.net.Session
import play.example.module.login.message.LoginParams
import play.mvc.Request
import play.mvc.Response
import play.util.control.Result2
import play.util.control.err
import play.util.control.getCause
import play.util.control.ok
import play.util.unsafeCast

class AccountManager(
  context: ActorContext<Command>,
  private val platformServiceProvider: PlatformServiceProvider,
  private val accountIdCache: AccountIdCache,
  loginDispatcher: ActorRef<LoginDispatcherActor.Command>,
  private val serverService: ServerService,
  private val accountCache: AccountEntityCache,
  private val playerManager: ActorRef<PlayerManager.Command>,
  private val playerEntityCacheInitializer: PlayerEntityCacheInitializer,
  private val playerService: PlayerService,
  private val actorMdc: ActorMDC
) : AbstractTypedActor<AccountManager.Command>(context) {

  init {
    val subscriber =
      context.messageAdapter(LoginDispatcherActor.UnhandledLoginRequest::class.java) {
        RequestCommand(
          it.request,
          it.session
        )
      }
    val serverIds = serverService.getServerIds()
    loginDispatcher.tell(LoginDispatcherActor.RegisterLoginReceiver(serverIds, subscriber))
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
      AccountModule.login -> login(request, session)
      AccountModule.ping -> {
        println("ping")
        session.write(Response(request.header, 0, "hello"))
      }

      else -> Log.warn { "unhandled request: $request" }
    }
  }

  private fun login(request: Request, session: Session) {
    val requestBody = request.body
    val params = MessageCodec.decode<LoginParams>(requestBody.getPayload())
    val result = getOrCreateAccount(params)
    if (result.isErr()) {
      session.write(Response(request.header, result.getErrorCode()))
      Log.error { "登录失败: errorCode=${result.getErrorCode()} params=$params" }
      return
    }
    val accountId = result.get()
    val accountActorName = accountId.toString()
    val maybeAccountActor = context.getChild(accountActorName)
    val accountActor = if (maybeAccountActor.isPresent) {
      maybeAccountActor.unsafeCast()
    } else {
      val behavior = Behaviors.withMdc(
        AccountActor.Command::class.java,
        actorMdc.staticMdc,
        actorMdc.mdcPerMessage(),
        AccountActor.create(accountId, accountCache, playerManager, playerService)
      )
      context.spawn(behavior, accountActorName)
    }
    accountActor send AccountActor.Login(request, params, session)
  }

  private fun getOrCreateAccount(params: LoginParams): Result2<Long> {
    val platformName = params.platform
    val platform = Platforms.getOrNull(platformName)
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
    val id = accountIdCache.getOrNull(accountId)
    if (id != null) {
      return ok(id)
    }
    // 账号不存在，新注册用户
    val maybeId = accountIdCache.nextId(platform.toByte(), serverId)
    if (maybeId.isEmpty) {
      return AccountErrorCode.IdExhausted
    }
    val account = platformService.newAccount(maybeId.asLong, platform.toByte(), serverId, params.account, params)
    accountIdCache.add(accountId, account.id)
    createAsync(account)
    return ok(maybeId.asLong)
  }

  private fun createAsync(account: Account) {
    val unsafeOps = accountCache.unsafeOps()
    unsafeOps.initWithEmptyValue(account.id)
    accountCache.create(account)
    Log.info { "账号创建成功: $account" }
    val id = account.id
    future {
      playerEntityCacheInitializer.initWithEmptyValue(id, unsafeOps)
    }.onComplete {
      if (it.isSuccess) {
        Log.info { "玩家实体缓存初始化完成: $id" }
      } else {
        Log.error(it.getCause()) { "玩家实体缓存初始化失败: $id" }
      }
    }
  }

  companion object {
    fun create(
      platformServiceProvider: PlatformServiceProvider,
      accountIdCache: AccountIdCache,
      loginDispatcher: ActorRef<LoginDispatcherActor.Command>,
      serverService: ServerService,
      accountCache: AccountEntityCache,
      playerManager: ActorRef<PlayerManager.Command>,
      playerEntityCacheInitializer: PlayerEntityCacheInitializer,
      playerService: PlayerService,
      actorMdc: ActorMDC
    ): Behavior<Command> {
      return Behaviors.setup { ctx ->
        AccountManager(
          ctx,
          platformServiceProvider,
          accountIdCache,
          loginDispatcher,
          serverService,
          accountCache,
          playerManager,
          playerEntityCacheInitializer,
          playerService,
          actorMdc
        )
      }
    }
  }

  interface Command
  private class RequestCommand(val request: Request, val session: Session) : Command
}
