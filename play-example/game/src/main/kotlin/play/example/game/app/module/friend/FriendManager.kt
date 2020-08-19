package play.example.game.app.module.friend

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.example.game.app.module.ModuleId
import play.example.game.app.module.guild.GuildManager
import play.example.game.app.module.player.PlayerRequestHandler
import play.mvc.AbstractPlayerRequest
import play.mvc.PlayerRequest
import play.mvc.RequestCommander

/**
 * 好友管理器
 * @author LiangZengle
 */
class FriendManager(
  ctx: ActorContext<Command>,
  private val requestHandler: PlayerRequestHandler
) : AbstractTypedActor<FriendManager.Command>(ctx) {

  init {
    requestHandler.register(
      context.messageAdapter(PlayerRequest::class.java, ::FriendPlayerRequest)
    ) { moduleId, _ -> moduleId == ModuleId.Friend.toInt() }
  }

  private val token = Token(self)

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onRequest)
      .build()
  }

  private fun onRequest(cmd: FriendPlayerRequest) {
    requestHandler.handle(Commander(cmd.playerId, token), cmd.request)
  }

  companion object {
    fun create(requestHandler: PlayerRequestHandler): Behavior<Command> {
      return Behaviors.setup { ctx -> FriendManager(ctx, requestHandler) }
    }
  }

  interface Command

  class FriendPlayerRequest(req: PlayerRequest) : AbstractPlayerRequest(req.playerId, req.request), Command

  inner class Token constructor(val friendManager: ActorRef<Command>)

  inner class Commander(override val id: Long, val token: Token) : RequestCommander()
}
