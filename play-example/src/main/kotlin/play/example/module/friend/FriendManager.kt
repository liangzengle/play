package play.example.module.friend

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.example.module.ModuleId
import play.example.module.player.PlayerRequest
import play.example.module.player.PlayerRequestHandler

/**
 * 好友管理器
 * @author LiangZengle
 */
class FriendManager(
  ctx: ActorContext<Command>,
  private val requestHandler: PlayerRequestHandler
) : AbstractTypedActor<FriendManager.Command>(ctx) {

  init {
    requestHandler.register(self.unsafeUpcast()) { moduleId, _ -> moduleId == ModuleId.Friend.toInt() }
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onRequest)
      .build()
  }

  private fun onRequest(cmd: PlayerRequest) {
    requestHandler.handle(cmd)
  }

  companion object {
    fun create(requestHandler: PlayerRequestHandler): Behavior<Command> {
      return Behaviors.setup { ctx -> FriendManager(ctx, requestHandler) }
    }
  }

  interface Command
}
