package play.example.game.app.module.friend.domain

import play.example.common.ModularCode
import play.example.common.StatusCode
import play.example.game.app.module.ModuleId

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
