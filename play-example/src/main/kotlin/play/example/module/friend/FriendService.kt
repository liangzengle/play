package play.example.module.friend

import play.example.module.friend.controller.FriendInfo
import play.example.module.friend.entity.PlayerFriendEntityCache
import play.util.control.Result2
import play.util.control.ok
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 好友模块逻辑处理
 */
@Singleton
class FriendService @Inject constructor(
  private val friendEntityCache: PlayerFriendEntityCache
) {
  fun getInfo(playerId: Long): FriendInfo {
    println("getInfo")
    return FriendInfo()
  }

  fun getInfo2(playerId: Long): Result2<FriendInfo> {
    println("getInfo2")
    return ok(FriendInfo())
  }
}
