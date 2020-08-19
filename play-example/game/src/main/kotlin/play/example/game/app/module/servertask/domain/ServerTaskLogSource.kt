package play.example.game.app.module.servertask.domain

import play.example.common.ModularCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.task.domain.TaskLogSource

/**
 * 全服任务日志源
 */
@ModularCode
public object ServerTaskLogSource : TaskLogSource(ModuleId.ServerTask) {
  override val TaskReward: Int = 1
}
