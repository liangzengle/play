package play.example.game.app.module.task.domain

import play.example.common.StatusCode
import play.util.control.Result2

/**
 * 任务错误码基类
 *
 * @author LiangZengle
 */
abstract class TaskErrorCode(moduleId: Short) : StatusCode(moduleId) {
  /**
   * 任务不存在
   */
  abstract val TaskNotExist: Result2<Nothing>

  /**
   * 任务奖励已领取
   */
  abstract val TaskRewarded: Result2<Nothing>

  /**
   * 任务未完成
   */
  abstract val TaskNotFinished: Result2<Nothing>
}
