package play.example.game.module.maintask.entity

import play.example.game.module.player.entity.AbstractPlayerEntity

/**
 * 主线任务数据
 */
public class MainTaskEntity(playerId: Long) : AbstractPlayerEntity(playerId) {

  /**
   * 当前任务
   */
  var task: PlayerMainTask? = null

}
