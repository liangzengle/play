package play.example.game.app.module.task.handler

import play.example.game.app.module.task.domain.CommonTaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.target.TaskTarget

/**
 * 通用任务目标处理器
 *
 * @author LiangZengle
 */
abstract class CommonTaskTargetHandler<Target : TaskTarget, Event : TaskEvent>(val type: CommonTaskTargetType) {

  /**
   * 获取任务初始化时的进度值
   *
   * @param target 任务目标
   * @param taskConfig 任务配置
   * @return 初始任务进度
   */
  abstract fun getInitialProgress(target: Target, taskConfig: AbstractTaskResource): Int

  /**
   * 任务事件触发
   *
   * @param target 任务目标
   * @param event 任务事件
   * @param currentProgress 当前任务进度
   * @param taskConfig 任务配置
   * @return 变化的任务进度
   */
  abstract fun onEvent(
    target: Target,
    event: Event,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int
}
