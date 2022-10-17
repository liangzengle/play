package play.example.game.app.module.activity.impl.task.domain

import play.example.game.app.module.ModuleId
import play.example.game.app.module.task.domain.TaskErrorCode
import play.util.control.Result2

/**
 *
 *
 * @author LiangZengle
 */
object TaskActivityErrorCode : TaskErrorCode(ModuleId.ActivityTask) {
  override val TaskNotExist: Result2<Nothing> = code(1)
  override val TaskRewarded: Result2<Nothing> = code(2)
  override val TaskNotFinished: Result2<Nothing> = code(3)
}
