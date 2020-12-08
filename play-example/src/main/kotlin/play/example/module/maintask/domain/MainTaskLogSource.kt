package play.example.module.maintask.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.task.domain.TaskLogSource

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
