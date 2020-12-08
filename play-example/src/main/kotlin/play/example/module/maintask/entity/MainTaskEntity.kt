package play.example.module.maintask.entity

import play.example.module.player.entity.PlayerEntity

/**
 * 主线任务数据
 */
public class MainTaskEntity(playerId: Long) : PlayerEntity(playerId) {

  /**
   * 当前任务
   */
  var task: PlayerMainTask? = null

}
