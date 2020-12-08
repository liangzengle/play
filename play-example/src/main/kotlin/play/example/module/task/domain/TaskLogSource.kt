package play.example.module.task.domain

import play.example.module.LogSource

/**
 * 任务错误码基类
 *
 * @author LiangZengle
 */
abstract class TaskLogSource(moduleId: Short) : LogSource(moduleId) {

  /**
   * 任务奖励
   */
  abstract val TaskReward: Int
}
