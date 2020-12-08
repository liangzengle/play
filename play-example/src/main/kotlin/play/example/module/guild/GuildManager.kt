package play.example.module.guild

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.StashBuffer
import java.util.*
import play.akka.AbstractTypedActor
import play.akka.accept
import play.akka.sameBehavior
import play.example.common.id.GameUIDGenerator
import play.example.module.ModuleId
import play.example.module.guild.config.GuildSettingConf
import play.example.module.guild.domain.GuildErrorCode
import play.example.module.guild.domain.GuildLogSource
import play.example.module.guild.entity.GuildEntity
import play.example.module.guild.entity.GuildEntityCache
import play.example.module.guild.message.*
import play.example.module.player.PlayerRequestHandler
import play.example.module.player.PlayerService
import play.example.module.player.event.PlayerEventBus
import play.example.module.player.event.PlayerExecCost
import play.example.module.reward.model.CostResultSet
import play.mvc.PlayerRequest
import play.mvc.RequestResult
import play.util.collection.ConcurrentLongLongMap
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
  private val playerEventBus: PlayerEventBus
) : AbstractTypedActor<GuildManager.Command>(ctx) {

  private val guildIdGenerator: GameUIDGenerator

  init {
    // 拦截工会相关的请求
    requestHandler
      .register(context.messageAdapter(PlayerRequest::class.java, GuildMessageConverter::convert)) { moduleId, _ ->
        moduleId == ModuleId.Guild.toInt()
      }

    guildIdGenerator = guildEntityCache.asSequence().maxOfOrNull { it.id }?.let { GameUIDGenerator.fromId(it) }
      ?: GameUIDGenerator.createDefault()

    guildEntityCache.asSequence().forEach { entity ->
      for (member in entity.members) {
        updatePlayerGuild(member, entity.id)
      }
    }
  }

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
    requestHandler.handle(req)
  }

  private fun joinGuild(cmd: JoinGuildRequest) {
    val result = joinGuild(cmd.playerId, cmd.guildId)
    requestHandler.onResult(cmd, RequestResult(result))
  }

  private fun joinGuild(playerId: Long, guildId: Long): Result2<GuildInfo> {
    if (hasGuild(playerId)) {
      return err(GuildErrorCode.HasGuild)
    }
    // 所有工会都在缓存中，不在说明没有这个工会，没必要读数据库
    val maybeGuild = guildEntityCache.getCached(guildId)
    if (maybeGuild.isEmpty) {
      return err(GuildErrorCode.GuildNotExists)
    }
    val guild = maybeGuild.get()
    guild.members.add(playerId)
    updatePlayerGuild(playerId, guildId)
    return ok(guild.toMessage())
  }

  private fun createGuild(cmd: CreateGuildRequest): Behavior<Command> {
    // 返回异步的创建结果
    val future = createGuildStart(cmd.playerId, cmd.guildName)
    requestHandler.onResult(cmd, RequestResult(future))
    // 如果future是complete状态，说明创建失败了
    // 否则切换到creatingBehavior，等待玩家消耗结果
    return if (future.isCompleted()) sameBehavior() else creatingBehavior
  }

  // 创建工会第1步：通知玩家扣除创建消耗
  private fun createGuildStart(
    playerId: Long,
    guildName: String
  ): PlayFuture<Result2<GuildInfo>> {
    if (hasGuild(playerId)) {
      return PlayFuture.successful(err(GuildErrorCode.HasGuild))
    }
    if (!isGuildNameAvailable(guildName)) {
      return PlayFuture.successful(err(GuildErrorCode.NameNotAvailable))
    }
    val promise = PlayPromise.make<Result2<GuildInfo>>()
    // 通过事件向PlayerActor发起消耗请求
    val costPromise = PlayPromise.make<Result2<CostResultSet>>()
    val costRequest =
      PlayerExecCost(playerId, GuildSettingConf.createCost, GuildLogSource.CreateGuild, costPromise)
    playerEventBus.post(costRequest)
    // 将消耗执行结果发送给自己
    costPromise.future.pipToSelf { CreateGuildCostResult(playerId, guildName, it, promise) }
    // 切换actor的行为，等待PlayerActor执行消耗的结果
    return promise.future
  }

  private fun isGuildNameAvailable(name: String) = guildEntityCache.asSequence().none { it.name == name }

  private fun onCostResult(cmd: CreateGuildCostResult): Behavior<Command> {
    cmd.promise.complete(
      runCatching {
        createGuildEnd(cmd.playerId, cmd.guildName, cmd.result)
      }
    )
    // 创建操作结束，切换到正常状态
    return stashBuffer.unstashAll(this)
  }

  // 创建工会第2步：处理玩家消耗扣除结果
  private fun createGuildEnd(
    playerId: Long,
    guildName: String,
    result: Result<Result2<CostResultSet>>
  ): Result2<GuildInfo> {
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
      val guildId = genGuildId()
      val guild = GuildEntity(guildId, guildName)
      guild.leaderId = playerId
      guild.leaderName = playerService.getPlayerName(playerId)
      guildEntityCache.create(guild)
      updatePlayerGuild(playerId, guildId)
      ok(guild.toMessage())
    }
  }

  /**
   * 生成一个新的工会id
   */
  private fun genGuildId(): Long = guildIdGenerator.nextOrThrow()

  companion object {
    private val playerIdToGuildId = ConcurrentLongLongMap()

    fun hasGuild(playerId: Long) = playerIdToGuildId.containsKey(playerId)

    fun updatePlayerGuild(playerId: Long, guildId: Long) {
      playerIdToGuildId.put(playerId, guildId)
    }

    fun changePlayerGuild(playerId: Long, fromGuildId: Long, toGuildId: Long) {
      if (playerIdToGuildId.remove(playerId, fromGuildId)) {
        playerIdToGuildId.put(playerId, toGuildId)
      } else {
        // TODO log
      }
    }

    fun getPlayerGuildId(playerId: Long): OptionalLong {
      val guildId = playerIdToGuildId[playerId]
      return guildId?.let { OptionalLong.of(guildId) } ?: OptionalLong.empty()
    }

    fun create(
      playerRequestHandler: PlayerRequestHandler,
      guildEntityCache: GuildEntityCache,
      playerService: PlayerService,
      playerEventBus: PlayerEventBus
    ): Behavior<Command> = Behaviors.setup { ctx ->
      Behaviors.withStash(Int.MAX_VALUE) { stash ->
        GuildManager(ctx, stash, playerRequestHandler, guildEntityCache, playerService, playerEventBus)
      }
    }
  }

  interface Command

  private class CreateGuildCostResult(
    val playerId: Long,
    val guildName: String,
    val result: Result<Result2<CostResultSet>>,
    val promise: PlayPromise<Result2<GuildInfo>>
  ) : Command
}
