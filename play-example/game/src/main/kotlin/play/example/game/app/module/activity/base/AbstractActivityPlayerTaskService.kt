package play.example.game.app.module.activity.base

import com.google.common.collect.Lists
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.playertask.AbstractPlayerTaskService
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.task.entity.TaskData
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.AbstractTaskResource
import play.util.control.Result2
import play.util.control.ok

/**
 *
 *
 * @author LiangZengle
 */
abstract class AbstractActivityPlayerTaskService<DATA : TaskData, CONFIG : AbstractTaskResource>(
  protected val entity: PlayerActivityEntity,
  targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  rewardService: RewardService
) : AbstractPlayerTaskService<DATA, CONFIG>(targetHandlerProvider, rewardService) {

  protected val activityId get() = entity.id.activityId

  abstract fun getTaskMap(): MutableIntObjectMap<DATA>

  abstract fun listTaskConfig(): List<CONFIG>

  override fun getTask(owner: PlayerManager.Self, taskId: Int): DATA? {
    return getTaskMap().get(taskId)
  }

  override fun getTasksInProgress(owner: PlayerManager.Self, event: TaskEvent): Collection<DATA> {
    val taskMap = getTaskMap()
    return Lists.transform(listTaskConfig()) { res ->
      taskMap.getIfAbsentPutWithKey(res.id) { createTask(owner, res) }
    }
  }

  override fun onTaskChanged(owner: PlayerManager.Self, changedTasks: List<DATA>) {

  }

  override fun checkAcceptConditions(owner: PlayerManager.Self, taskConf: CONFIG): Result2<Nothing> {
    return ok()
  }

  override fun addTask(owner: PlayerManager.Self, task: DATA) {
    getTaskMap().put(task.id, task)
  }
}
