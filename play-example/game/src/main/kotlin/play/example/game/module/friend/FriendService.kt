package play.example.game.module.friend

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.friend.entity.PlayerFriendEntityCache
import play.example.game.module.friend.message.FriendInfo
import play.util.control.Result2
import play.util.control.ok

/**
 * 好友模块逻辑处理
 */
@Singleton
class FriendService @Inject constructor(
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
