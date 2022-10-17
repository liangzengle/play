package play.example.game.app.module.maintask.entity

import play.example.game.app.module.player.entity.AbstractPlayerEntity
import play.example.game.app.module.task.entity.TaskData

/**
 * 主线任务数据
 */
public class MainTaskEntity(playerId: Long) : AbstractPlayerEntity(playerId) {

  /**
   * 当前任务
   */
  var task: TaskData? = null

}
