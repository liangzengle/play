package play.example.game.app.module.maintask

import org.springframework.stereotype.Component
import play.example.game.app.module.maintask.domain.MainTaskErrorCode
import play.example.game.app.module.maintask.domain.MainTaskLogSource
import play.example.game.app.module.maintask.entity.MainTaskEntityCache
import play.example.game.app.module.maintask.res.MainTaskResource
import play.example.game.app.module.maintask.res.MainTaskResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.AbstractPlayerTaskService
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.entity.TaskData
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.TaskResourceExtension
import play.example.module.task.message.TaskProto
import play.util.filterOrNull

/**
 * 主线任务模块逻辑处理
 */
@Component
class MainTaskService(
  private val mainTaskEntityCache: MainTaskEntityCache,
  targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  rewardService: RewardService
) : AbstractPlayerTaskService<TaskData, MainTaskResource>(
  targetHandlerProvider,
  rewardService
) {

  override fun getResourceExtension(): TaskResourceExtension<MainTaskResource>? {
    return MainTaskResourceSet.extension()
  }

  /**
   * 添加任务
   *
   * @param owner 自己
   * @param task 新任务
   */
  override fun addTask(owner: Self, task: TaskData) {
    val entity = mainTaskEntityCache.getOrCreate(owner.id)
    entity.task = task
  }

  /**
   * 创建一个玩家任务对象
   *
   * @param owner 自己
   * @param taskConf 任务配置
   * @return PlayerTask
   */
  override fun createTask(owner: Self, taskConf: MainTaskResource): TaskData {
    return TaskData(taskConf.id)
  }

  /**
   * 获取自己的某个任务
   *
   * @param owner 自己
   * @param taskId 任务id
   * @return PlayerTask?
   */
  override fun getTask(owner: Self, taskId: Int): TaskData? {
    return mainTaskEntityCache.getOrNull(owner.id)?.task
  }

  /**
   * 检查任务是否接取
   *
   * @param owner 自己
   * @param taskConf 任务配置
   * @return 状态码
   */
  override fun checkAcceptConditions(owner: Self, taskConf: MainTaskResource) = MainTaskErrorCode.Success

  /**
   * 任务进度发生变化时
   *
   * @param owner 自己
   * @param changedTasks 发生变化的任务
   */
  override fun onTaskChanged(owner: Self, changedTasks: List<TaskData>) {
  }

  /**
   * 获取玩家进行中的任务
   *
   * @param owner 自己
   * @param event 当前触发的任务事件
   * @return 进行中的任务
   */
  override fun getTasksInProgress(owner: Self, event: TaskEvent): Collection<TaskData> {
    return mainTaskEntityCache
      .getOrNull(owner.id)?.task?.filterOrNull { it.isInProgress() }?.let { listOf(it) } ?: emptyList()
  }

  /**
   * 获取任务配置
   *
   * @param taskId 任务id
   * @return 任务配置 or null
   */
  override fun getTaskConfig(taskId: Int): MainTaskResource? {
    return MainTaskResourceSet.getOrNull(taskId)
  }

  fun toMessage(task: TaskData): TaskProto {
    return TaskProto(task.id, task.status.toInt(), getProgressList(task))
  }

  override val errorCode: TaskErrorCode
    get() = MainTaskErrorCode
  override val logSource: TaskLogSource
    get() = MainTaskLogSource

}
