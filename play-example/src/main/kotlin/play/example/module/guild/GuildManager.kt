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
import play.example.common.net.message.ok
import play.example.module.ModuleId
import play.example.module.guild.controller.GuildControllerInvoker
import play.example.module.guild.entity.GuildEntity
import play.example.module.guild.entity.GuildEntityCache
import play.example.module.guild.message.GuildProto
import play.example.module.player.PlayerRequest
import play.example.module.player.PlayerRequestHandler
import play.example.module.player.PlayerService
import play.mvc.RequestResult
import play.util.concurrent.Promise

/**
 * Actor handling guild requests
 * @author LiangZengle
 */
class GuildManager(
  ctx: ActorContext<Command>,
  private val stashBuffer: StashBuffer<Command>,
  private val playerRequestHandler: PlayerRequestHandler,
  private val guildEntityCache: GuildEntityCache,
  private val playerService: PlayerService
) : AbstractTypedActor<GuildManager.Command>(ctx) {

  private val guildIdGenerator: GameUIDGenerator

  init {
    playerRequestHandler
      .register(context.messageAdapter(PlayerRequest::class.java, ::GuildPlayerRequest))
      { moduleId, _ -> moduleId == ModuleId.Guild.toInt() }

    guildIdGenerator = guildEntityCache.asSequence().maxOfOrNull { it.id }?.let { GameUIDGenerator.fromId(it) }
      ?: GameUIDGenerator.createDefault()
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
      .build()
  }

  private fun onRequest(req: GuildPlayerRequest): Behavior<Command> {
    playerRequestHandler.handle(req.request)
    // change behavior
    return if (req.request.request.header.msgId.toInt() == GuildControllerInvoker.create) {
      creatingBehavior
    } else {
      sameBehavior()
    }
  }

  /**
   * 创建工会
   */
  private fun createGuild(cmd: CreateGuildCommand): Behavior<Command> {
    cmd.promise.complete {
      RequestResult.ok {
        val guildId = genGuildId()
        val guild = GuildEntity(guildId, cmd.guildName)
        guild.leaderId = cmd.playerId
        guild.leaderName = playerService.getPlayerName(cmd.playerId)
        guildEntityCache.create(guild)
        GuildProto(guild.id, guild.name, guild.leaderId, guild.leaderName, 1)
      }
    }
    return stashBuffer.unstashAll(this)
  }


  /**
   * 生成一个新的工会id
   */
  private fun genGuildId(): Long = guildIdGenerator.nextOrThrow()

  companion object {
    fun create(
      playerRequestHandler: PlayerRequestHandler,
      guildEntityCache: GuildEntityCache,
      playerService: PlayerService
    ): Behavior<Command> = Behaviors.setup { ctx ->
      Behaviors.withStash(Int.MAX_VALUE) { stash ->
        GuildManager(ctx, stash, playerRequestHandler, guildEntityCache, playerService)
      }
    }
  }

  interface Command

  private class GuildPlayerRequest(val request: PlayerRequest) : Command

  class CreateGuildCommand(val playerId: Long, val guildName: String, val promise: Promise<RequestResult<GuildProto>>) :
    Command
}
