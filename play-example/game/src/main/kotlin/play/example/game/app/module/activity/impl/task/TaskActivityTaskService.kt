package play.example.game.app.module.activity.impl.task

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.example.game.app.module.activity.base.AbstractActivityPlayerTaskService
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.impl.task.domain.TaskActivityErrorCode
import play.example.game.app.module.activity.impl.task.domain.TaskActivityLogSource
import play.example.game.app.module.activity.impl.task.res.TaskActivityResource
import play.example.game.app.module.activity.impl.task.res.TaskActivityResourceSet
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.entity.TaskData
import play.example.game.app.module.task.res.TaskResourceExtension

/**
 *
 *
 * @author LiangZengle
 */
class TaskActivityTaskService(
  entity: PlayerActivityEntity,
  targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  rewardService: RewardService
) : AbstractActivityPlayerTaskService<TaskData, TaskActivityResource>(
  entity,
  targetHandlerProvider,
  rewardService
) {

  override val errorCode: TaskErrorCode = TaskActivityErrorCode

  override val logSource: TaskLogSource = TaskActivityLogSource

  override fun getTaskMap(): MutableIntObjectMap<TaskData> {
    return entity.getSimpleTaskData()
  }

  override fun getResourceExtension(): TaskResourceExtension<TaskActivityResource> {
    return TaskActivityResourceSet.extension()
  }

  override fun getTaskConfig(taskId: Int): TaskActivityResource? {
    return TaskActivityResourceSet.getOrNull(taskId)
  }

  override fun listTaskConfig(): List<TaskActivityResource> {
    return TaskActivityResourceSet.getGroupOrNull(activityId)?.list() ?: emptyList()
  }

  override fun createTask(owner: PlayerManager.Self, taskConf: TaskActivityResource): TaskData {
    return TaskData(taskConf.id)
  }

  override fun onTaskFinished(owner: PlayerManager.Self, task: TaskData, taskConf: TaskActivityResource) {
    super.onTaskFinished(owner, task, taskConf)
    logger.debug { "活动任务完成: $owner $taskConf $task" }
  }
}
