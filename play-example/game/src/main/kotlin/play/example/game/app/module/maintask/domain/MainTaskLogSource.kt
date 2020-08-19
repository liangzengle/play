package play.example.game.app.module.maintask.domain

import play.example.common.ModularCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.task.domain.TaskLogSource

/**
 * 主线任务日志源
 */
@ModularCode
@SuppressWarnings("MayBeConstant")
public object MainTaskLogSource : TaskLogSource(ModuleId.MainTask) {
  /**
   * 主线任务奖励
   */
  override val TaskReward: Int = 1
}
