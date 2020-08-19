package play.example.game.app.module.servertask.domain

import play.example.common.ModularCode
import play.example.game.app.module.ModuleId
import play.example.game.app.module.task.domain.TaskErrorCode
import play.util.control.Result2

/**
 * 全服任务错误码
 */
@ModularCode
public object ServerTaskErrorCode : TaskErrorCode(ModuleId.ServerTask) {
  override val TaskNotExist: Result2<Nothing> = code(1)
  override val TaskRewarded: Result2<Nothing> = code(2)
  override val TaskNotFinished: Result2<Nothing> = code(3)
}
