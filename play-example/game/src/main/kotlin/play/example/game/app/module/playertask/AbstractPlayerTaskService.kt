package play.example.game.app.module.playertask

import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.event.IPlayerTaskEvent
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.model.RewardResultSet
import play.example.game.app.module.task.AbstractTaskService
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.entity.TaskData
import play.example.game.app.module.task.res.AbstractTaskResource
import play.util.control.Result2

/**
 * 任务模块服务
 *
 * @author LiangZengle
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractPlayerTaskService<DATA : TaskData, CONFIG : AbstractTaskResource>(
  protected val targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  protected val rewardService: RewardService
) : AbstractTaskService<Self, DATA, CONFIG, IPlayerTaskEvent>() {

  /**
   * 获取目标处理器
   *
   * @param targetType 任务目标类型
   */
  override fun getHandlerOrNull(targetType: TaskTargetType) = targetHandlerProvider.getOrNull(targetType)

  /**
   * 领取任务奖励
   *
   * @param self 自己
   * @param taskId 任务id
   * @return 奖励结果
   */
  open fun getTaskReward(self: Self, taskId: Int): Result2<RewardResultSet> {
    val taskConfig = getTaskConfig(taskId) ?: return errorCode.ResourceNotFound
    val playerTask = getTask(self, taskId) ?: return errorCode.TaskNotExist
    if (playerTask.isRewarded()) {
      return errorCode.TaskRewarded
    }
    if (!playerTask.isFinished()) {
      return errorCode.TaskNotFinished
    }
    val rewards = getRewards(self, taskConfig)
    val rewardResult = rewardService.tryAndExecReward(self, rewards, logSource.TaskReward)
    if (rewardResult.isOk()) {
      playerTask.setRewarded()
      onTaskRewarded(self, playerTask, taskConfig)
    }
    return rewardResult
  }


  /**
   * 错误码
   */
  abstract val errorCode: TaskErrorCode

  /**
   * 日志源
   */
  abstract val logSource: TaskLogSource

}
