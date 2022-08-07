package play.example.game.app.module.friend

import akka.actor.typed.ActorRef
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import play.example.game.app.module.friend.entity.PlayerFriendEntityCache
import play.example.module.friend.message.FriendInfo
import play.util.control.Result2
import play.util.control.ok

/**
 * 好友模块逻辑处理
 */
@Component
class FriendService(
  private val friendManagerProvider: ObjectProvider<ActorRef<FriendManager.Command>>,
  private val friendEntityCache: PlayerFriendEntityCache
) {
  fun getInfo(playerId: Long): Result2<FriendInfo> {
    println("getInfo")
    return ok(FriendInfo())
  }

  fun getInfo2(playerId: Long): Result2<FriendInfo> {
    println("getInfo2")
    return ok(FriendInfo())
  }
}
