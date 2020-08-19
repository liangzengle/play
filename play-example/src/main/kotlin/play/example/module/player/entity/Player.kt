package play.example.module.player.entity

/**
 * 存放玩家的私人数据
 */
class Player(id: Long, val name: String, val ctime: Long) : PlayerEntity(id) {

  // 新的天时间
  var dtime = 0L

  // 新的周时间
  var wtime = 0L

  // 新的月时间
  var mtime = 0L
}
