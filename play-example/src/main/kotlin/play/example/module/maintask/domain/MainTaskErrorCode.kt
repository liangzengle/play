package play.example.module.maintask.domain

import play.example.module.ModularCode
import play.example.module.ModuleId
import play.example.module.task.domain.TaskErrorCode
import play.util.control.Result2

/**
 * 主线任务错误码
 */
@ModularCode
@SuppressWarnings("MayBeConstant")
public object MainTaskErrorCode : TaskErrorCode(ModuleId.MainTask) {
  /**
   * 任务不存在
   */
  override val PlayerTaskNotExist: Result2<Nothing> = code(1)

  /**
   * 任务奖励已领取
   */
  override val TaskRewarded: Result2<Nothing> = code(2)

  /**
   * 任务未完成
   */
  override val TaskNotFinished: Result2<Nothing> = code(3)

}
