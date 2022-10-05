package play.example.game.app.module.guild

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.StashBuffer
import play.akka.AbstractTypedActor
import play.example.game.app.module.ModuleId
import play.example.game.app.module.guild.domain.GuildErrorCode
import play.example.game.app.module.guild.domain.GuildLogSource
import play.example.game.app.module.guild.entity.GuildEntity
import play.example.game.app.module.guild.entity.GuildEntityCache
import play.example.game.app.module.guild.message.CreateGuildRequest
import play.example.game.app.module.guild.message.GuildMessageConverter
import play.example.game.app.module.guild.message.GuildPlayerRequest
import play.example.game.app.module.guild.message.JoinGuildRequest
import play.example.game.app.module.guild.res.GuildSettingConf
import play.example.game.app.module.player.PlayerRequestHandler
import play.example.game.app.module.player.PlayerService
import play.example.game.app.module.reward.model.CostResultSet
import play.example.module.guild.message.GuildProto
import play.mvc.PlayerRequest
import play.mvc.RequestCommander
import play.mvc.RequestResult
import play.util.concurrent.PlayFuture
import play.util.concurrent.PlayPromise
import play.util.control.Result2
import play.util.control.err
import play.util.control.getCause
import play.util.control.ok

/**
 * Actor handling guild requests
 * @author LiangZengle
 */
class GuildManager(
  ctx: ActorContext<Command>,
  private val stashBuffer: StashBuffer<Command>,
  private val requestHandler: PlayerRequestHandler,
  private val guildEntityCache: GuildEntityCache,
  private val playerService: PlayerService,
  private val guildCache: GuildCache
) : AbstractTypedActor<GuildManager.Command>(ctx) {

  init {
    // 拦截工会相关的请求
    requestHandler
      .register(context.messageAdapter(PlayerRequest::class.java, GuildMessageConverter::convert)) { moduleId, _ ->
        moduleId == ModuleId.Guild.toInt()
      }
  }

  private val token = Token(self)

  /**
   * 创建工会中，只处理CreateGuildCommand消息，其他消息先stash
   */
  private val creatingBehavior: Behavior<Command> = newBehaviorBuilder()
    .accept(::onCostResult) // create and unstashAll
    .accept(stashBuffer::stash) // stash other messages
    .build()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::joinGuild)
      .accept(::createGuild)
      .accept(::onRequest) // put it last
      .build()
  }

  /**
   * 处理工会请求
   * @param req 工会请求
   * @return Behavior<Command>
   */
  private fun onRequest(req: GuildPlayerRequest) {
    requestHandler.handle(Commander(req.playerId, token), req.request)
  }

  private fun joinGuild(cmd: JoinGuildRequest) {
    val result = joinGuild(cmd.playerId, cmd.guildId)
    requestHandler.onResult(cmd, RequestResult(result))
  }

  private fun joinGuild(playerId: Long, guildId: Long): Result2<GuildProto> {
    if (guildCache.hasGuild(playerId)) {
      return err(GuildErrorCode.HasGuild)
    }
    // 所有工会都在缓存中，不在说明没有这个工会，没必要读数据库
    val maybeGuild = guildEntityCache.getCached(guildId)
    if (maybeGuild.isEmpty) {
      return err(GuildErrorCode.GuildNotExists)
    }
    val guild = maybeGuild.get()
    guild.members.add(playerId)
    guildCache.updatePlayerGuild(playerId, guildId)
    return ok(guild.toMessage())
  }

  private fun createGuild(cmd: CreateGuildRequest): Behavior<Command> {
    // 返回异步的创建结果
    val future = createGuildStart(cmd.playerId, cmd.guildName)
    requestHandler.onResult(cmd, RequestResult.async { future })
    // 如果future是complete状态，说明创建失败了
    // 否则切换到creatingBehavior，等待玩家消耗结果
    return if (future.isCompleted()) sameBehavior() else creatingBehavior
  }

  // 创建工会第1步：通知玩家扣除创建消耗
  private fun createGuildStart(
    playerId: Long,
    guildName: String
  ): PlayFuture<Result2<GuildProto>> {
    if (guildCache.hasGuild(playerId)) {
      return PlayFuture.successful(err(GuildErrorCode.HasGuild))
    }
    if (!isGuildNameAvailable(guildName)) {
      return PlayFuture.successful(err(GuildErrorCode.NameNotAvailable))
    }
    val promise = PlayPromise.make<Result2<GuildProto>>()
    // 玩家消耗
    val costFuture = playerService.costAsync(playerId, GuildSettingConf.createCost, GuildLogSource.CreateGuild)
    // 将消耗执行结果发送给自己
    costFuture.pipToSelf { CreateGuildCostResult(playerId, guildName, it, promise) }
    // 切换actor的行为，等待PlayerActor执行消耗的结果
    return promise.future
  }

  private fun isGuildNameAvailable(name: String) = guildEntityCache.getAll().none { it.name == name }

  private fun onCostResult(cmd: CreateGuildCostResult): Behavior<Command> {
    cmd.promise.catchingComplete {
      createGuildEnd(cmd.playerId, cmd.guildName, cmd.result)
    }
    // 创建操作结束，切换到正常状态
    return stashBuffer.unstashAll(this)
  }

  // 创建工会第2步：处理玩家消耗扣除结果
  private fun createGuildEnd(
    playerId: Long,
    guildName: String,
    result: Result<Result2<CostResultSet>>
  ): Result2<GuildProto> {
    // 处理异常
    if (result.isFailure) {
      log.error("创建工会失败，玩家消耗执行异常", result.getCause())
      return GuildErrorCode.Failure
    }
    // 处理消耗结果
    val costResult = result.getOrThrow()
    return if (costResult.isErr()) {
      costResult.asErrResult()
    } else {
      // 消耗扣除成功后创建工会
      val guildId = guildCache.genGuildId()
      val guild = GuildEntity(guildId, guildName)
      guild.leaderId = playerId
      guild.leaderName = playerService.getPlayerNameOrThrow(playerId)
      guild.members.add(playerId)
      guildEntityCache.unsafeOps().initWithEmptyValue(guild.id)
      guildEntityCache.create(guild)
      guildCache.updatePlayerGuild(playerId, guildId)
      ok(guild.toMessage())
    }
  }

  companion object {
    fun create(
      playerRequestHandler: PlayerRequestHandler,
      guildEntityCache: GuildEntityCache,
      playerService: PlayerService,
      guildCache: GuildCache
    ): Behavior<Command> = Behaviors.setup { ctx ->
      Behaviors.withStash(Int.MAX_VALUE) { stash ->
        GuildManager(ctx, stash, playerRequestHandler, guildEntityCache, playerService, guildCache)
      }
    }
  }

  interface Command

  private class CreateGuildCostResult(
    val playerId: Long,
    val guildName: String,
    val result: Result<Result2<CostResultSet>>,
    val promise: PlayPromise<Result2<GuildProto>>
  ) : Command

  inner class Token constructor(val guildManager: ActorRef<Command>)

  inner class Commander(override val id: Long, val token: GuildManager.Token) : RequestCommander()
}
