package play.example.module.player.entity

/**
 * 存放玩家的公开数据
 */
class PlayerInfo(id: Long, var name: String) : PlayerEntity(id) {

  // 上次登录时间
  var lastLoginTime = 0L

  // 上次等录
  var lastLogoutTime = 0L
}
