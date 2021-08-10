package play.example.game.app.module.maintask.entity

import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

/**
 * 主线任务数据
 */
public class MainTaskEntity(playerId: Long) : AbstractPlayerLongIdEntity(playerId) {

  /**
   * 当前任务
   */
  var task: PlayerMainTask? = null

}
