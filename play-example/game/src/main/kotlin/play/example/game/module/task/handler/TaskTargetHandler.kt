package play.example.game.module.task.handler

import play.example.game.module.player.Self
import play.example.game.module.task.config.AbstractTaskConfig
import play.example.game.module.task.domain.TaskTargetType
import play.example.game.module.task.event.TaskEvent
import play.example.game.module.task.target.TaskTarget

/**
 * 任务目标处理
 *
 * @author LiangZengle
 */
abstract class TaskTargetHandler<Target : TaskTarget, Event : TaskEvent>(val type: TaskTargetType) {

  /**
   * 返回初始任务进度值
   *
   * @param self 玩家自己
   * @param target 任务目标
   * @param taskConfig 任务配置
   * @return 初始任务进度
   */
  abstract fun getInitialProgress(self: Self, target: Target, taskConfig: AbstractTaskConfig): Int

  /**
   * 任务事件触发
   *
   * @param self 玩家自己
   * @param target 任务目标
   * @param event 任务事件
   * @param currentProgress 当前任务进度
   * @param taskConfig 任务配置
   * @return 变化的任务进度
   */
  abstract fun onEvent(
    self: Self,
    target: Target,
    event: Event,
    currentProgress: Int,
    taskConfig: AbstractTaskConfig
  ): Int
}
