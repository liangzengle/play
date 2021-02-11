package play.example.module.player.entity

/**
 * 存放玩家的公开数据
 */
class Player2(id: Long, var name: String) : AbstractPlayerEntity(id) {

  // 上次登录时间
  var lastLoginTime = 0L

  // 上次登出时间
  var lastLogoutTime = 0L
}
