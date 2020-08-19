package play.example.module.friend.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.StatusCode

/**
 * 好友错误码
 */
@ModularCode
object FriendErrorCode : StatusCode(ModuleId.Friend) {

  // 好友人数已达上限
  val FriendCountLimit = 1
}
