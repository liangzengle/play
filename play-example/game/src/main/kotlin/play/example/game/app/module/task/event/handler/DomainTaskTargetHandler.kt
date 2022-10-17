package play.example.game.app.module.task.event.handler

import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.target.TaskTarget

/**
 * 特定任务目标处理
 *
 * @author LiangZengle
 */
interface DomainTaskTargetHandler<O, Target : TaskTarget, Event : TaskEvent> {

  // for covariant return type
  fun targetType(): TaskTargetType

  /**
   * 获取任务初始化时的进度值
   *
   * @param owner 任务归属者
   * @param target 任务目标
   * @param taskConfig 任务配置
   * @return 初始任务进度
   */
  abstract fun getInitialProgress(owner: O, target: Target, taskConfig: AbstractTaskResource): Int

  /**
   * 任务事件触发
   *
   * @param owner 任务归属者
   * @param target 任务目标
   * @param event 任务事件
   * @param currentProgress 当前任务进度
   * @param taskConfig 任务配置
   * @return 变化的任务进度
   */
  abstract fun onEvent(
    owner: O,
    target: Target,
    event: Event,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int
}
