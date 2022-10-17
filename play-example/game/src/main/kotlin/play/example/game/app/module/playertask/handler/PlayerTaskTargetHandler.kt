package play.example.game.app.module.playertask.handler

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.domain.PlayerTaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.event.handler.DomainTaskTargetHandler
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.target.TaskTarget

/**
 * 玩家任务目标处理
 *
 * @author LiangZengle
 */
abstract class PlayerTaskTargetHandler<Target : TaskTarget, Event : TaskEvent>(val targetType: PlayerTaskTargetType) :
  DomainTaskTargetHandler<Self, Target, Event> {

  override fun targetType(): PlayerTaskTargetType = targetType

  /**
   * 获取任务初始化时的进度值
   *
   * @param owner 玩家自己
   * @param target 任务目标
   * @param taskConfig 任务配置
   * @return 初始任务进度
   */
  abstract override fun getInitialProgress(owner: Self, target: Target, taskConfig: AbstractTaskResource): Int

  /**
   * 任务事件触发
   *
   * @param owner 玩家自己
   * @param target 任务目标
   * @param event 任务事件
   * @param currentProgress 当前任务进度
   * @param taskConfig 任务配置
   * @return 变化的任务进度
   */
  abstract override fun onEvent(
    owner: Self,
    target: Target,
    event: Event,
    currentProgress: Int,
    taskConfig: AbstractTaskResource
  ): Int
}
