package play.example.module.friend.domain

import play.example.module.LogSource
import play.example.module.ModularCode
import play.example.module.ModuleId

/**
 * 好友日志源
 */
@ModularCode
object FriendLogSource : LogSource(ModuleId.Friend)
