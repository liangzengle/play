package play.example.game.module.friend.domain

import play.example.common.LogSource
import play.example.common.ModularCode
import play.example.game.module.ModuleId

/**
 * 好友日志源
 */
@ModularCode
object FriendLogSource : LogSource(ModuleId.Friend)
