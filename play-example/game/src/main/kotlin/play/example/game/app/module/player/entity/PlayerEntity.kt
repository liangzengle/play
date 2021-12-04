package play.example.game.app.module.player.entity

/**
 * 存放玩家的私人数据
 *
 * @property ctime Long 创建时间
 * @property dtime Long 每日重置时间
 * @property wtime Long 每周重置时间
 * @property mtime Long 每月重置时间
 * @constructor
 */
class PlayerEntity(id: Long, val ctime: Long) : AbstractPlayerEntity(id) {

  // 新的天时间
  var dtime = 0L

  // 新的周时间
  var wtime = 0L

  // 新的月时间
  var mtime = 0L

  // 每日重置時間
  var dailyResetTime = 0L
}
