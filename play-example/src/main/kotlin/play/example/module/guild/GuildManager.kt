package play.example.module.guild

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.StashBuffer
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
import play.example.module.guild.message.GuildProto
import play.example.module.player.PlayerRequest
import play.example.module.player.PlayerRequestHandler
import play.example.module.player.PlayerService
import play.example.module.player.event.PlayerEventBus
import play.example.module.player.event.PlayerExecCost
import play.example.module.reward.model.CostResultSet
import play.util.collection.ConcurrentLongLongMap
import play.util.concurrent.PlayPromise
import play.util.control.Result2
import play.util.control.err
import play.util.control.getCause
import play.util.control.ok
import play.util.unsafeCast
import java.util.*

/**
 * Actor handling guild requests
 * @author LiangZengle
 */
class GuildManager(
  ctx: ActorContext<Command>,
  private val stashBuffer: StashBuffer<Command>,
  private val playerRequestHandler: PlayerRequestHandler,
  private val guildEntityCache: GuildEntityCache,
  private val playerService: PlayerService,
  private val playerEventBus: PlayerEventBus
) : AbstractTypedActor<GuildManager.Command>(ctx) {

  private val guildIdGenerator: GameUIDGenerator

  init {
    // 拦截工会相关的请求
    playerRequestHandler
      .register(
        context.messageAdapter(
          PlayerRequest::class.java,
          ::GuildPlayerRequest
        )
      ) { moduleId, _ -> moduleId == ModuleId.Guild.toInt() }

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
    .accept(::createGuild) // create and unstashAll
    .accept(stashBuffer::stash) // stash other messages
    .build()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onRequest)
      .accept(::create)
      .build()
  }

  /**
   * 处理工会请求
   * @param req 工会请求
   * @return Behavior<Command>
   */
  private fun onRequest(req: GuildPlayerRequest) {
    playerRequestHandler.handle(req.request)
  }

  private fun isGuildNameAvailable(name: String) = guildEntityCache.asSequence().none { it.name == name }

  private fun create(cmd: CreateGuildRequest): Behavior<Command> {
    val playerId = cmd.playerId
    val guildName = cmd.guildName
    val promise = cmd.promise

    if (hasGuild(playerId)) {
      promise.success(err(GuildErrorCode.HasGuild))
      return sameBehavior()
    }
    if (!isGuildNameAvailable(guildName)) {
      promise.success(err(GuildErrorCode.NameNotAvailable))
      return sameBehavior()
    }
    // 通过事件向PlayerActor发起消耗请求
    val costPromise = PlayPromise.make<Result2<CostResultSet>>()
    val costRequest =
      PlayerExecCost(playerId, GuildSettingConf.createCost, GuildLogSource.CreateGuild, costPromise)
    playerEventBus.post(costRequest)
    // 将消耗执行结果发送给自己
    costPromise.future.pipToSelf { CreateGuildCostResult(playerId, guildName, it, promise) }
    // 切换actor的行为，等待PlayerActor执行消耗的结果
    return creatingBehavior
  }

  private fun createGuild(cmd: CreateGuildCostResult): Behavior<Command> {
    try {
      val result = cmd.result
      val promise = cmd.promise
      // 处理异常
      if (result.isFailure) {
        log.error("玩家消耗执行异常", result.getCause())
        promise.success(err(GuildErrorCode.Failure))
      } else {
        // 处理消耗结果
        val costResult = result.getOrThrow()
        if (costResult.isErr()) {
          promise.success(costResult.unsafeCast())
        } else {
          // 消耗扣除成功后创建工会
          val guildId = genGuildId()
          val guild = GuildEntity(guildId, cmd.guildName)
          guild.leaderId = cmd.playerId
          guild.leaderName = playerService.getPlayerName(cmd.playerId)
          guildEntityCache.create(guild)
          updatePlayerGuild(cmd.playerId, guildId)
          promise.success(ok(guild.toProto()))
        }
      }
    } finally {
      // 将actor行为切换回正常状态, 同时释放stashBuffer中的消息
      return stashBuffer.unstashAll(this)
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

  private class GuildPlayerRequest(val request: PlayerRequest) : Command

  private class CreateGuildCostResult(
    val playerId: Long,
    val guildName: String,
    val result: Result<Result2<CostResultSet>>,
    val promise: PlayPromise<Result2<GuildProto>>
  ) : Command

  class CreateGuildRequest(val playerId: Long, val guildName: String, val promise: PlayPromise<Result2<GuildProto>>) :
    Command
}
