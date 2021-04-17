package play.example.game.module.friend.domain

import play.example.common.ModularCode
import play.example.game.module.ModuleId
import play.example.common.StatusCode

/**
 * 好友错误码
 */
@Suppress("MayBeConstant")
@ModularCode
object FriendErrorCode : StatusCode(ModuleId.Friend) {

  /**
   * 好友人数已达上限
   */
  val FriendCountLimit = code(1)
}
