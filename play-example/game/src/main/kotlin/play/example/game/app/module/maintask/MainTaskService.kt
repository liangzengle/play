package play.example.game.app.module.maintask

import org.springframework.stereotype.Component
import play.example.game.app.module.maintask.domain.MainTaskErrorCode
import play.example.game.app.module.maintask.domain.MainTaskLogSource
import play.example.game.app.module.maintask.entity.MainTaskEntityCache
import play.example.game.app.module.maintask.entity.PlayerMainTask
import play.example.game.app.module.maintask.res.MainTaskResource
import play.example.game.app.module.maintask.res.MainTaskResourceSet
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.playertask.AbstractPlayerTaskService
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.playertask.message.TaskInfo
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.AbstractTaskResourceExtension
import play.util.filterOrNull

/**
 * 主线任务模块逻辑处理
 */
@Component
public class MainTaskService(
  private val mainTaskEntityCache: MainTaskEntityCache,
  targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  rewardService: RewardService
) : AbstractPlayerTaskService<PlayerMainTask, MainTaskResource>(
  targetHandlerProvider,
  rewardService
) {

  override fun getResourceExtension(): AbstractTaskResourceExtension<MainTaskResource>? {
    return MainTaskResourceSet.extension();
  }

  /**
   * 添加任务
   *
   * @param self 自己
   * @param task 新任务
   */
  override fun addTask(self: Self, task: PlayerMainTask) {
    val entity = mainTaskEntityCache.getOrCreate(self.id)
    entity.task = task
  }

  /**
   * 创建一个玩家任务对象
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return PlayerTask
   */
  override fun createTask(self: Self, taskConf: MainTaskResource): PlayerMainTask {
    return PlayerMainTask(taskConf.id)
  }

  /**
   * 获取自己的某个任务
   *
   * @param self 自己
   * @param taskId 任务id
   * @return PlayerTask?
   */
  override fun getTask(self: Self, taskId: Int): PlayerMainTask? {
    return mainTaskEntityCache.getOrNull(self.id)?.task
  }

  /**
   * 检查任务是否接取
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return 状态码
   */
  override fun checkAcceptConditions(self: Self, taskConf: MainTaskResource) = MainTaskErrorCode.Success

  /**
   * 任务进度发生变化时
   *
   * @param self 自己
   * @param changedTasks 发生变化的任务
   */
  override fun onTaskChanged(self: Self, changedTasks: List<PlayerMainTask>) {
  }

  /**
   * 获取玩家进行中的任务
   *
   * @param self 自己
   * @param event 当前触发的任务事件
   * @return 进行中的任务
   */
  override fun getTasksInProgress(self: Self, event: TaskEvent): Collection<PlayerMainTask> {
    return mainTaskEntityCache
      .getOrNull(self.id)?.task?.filterOrNull { it.isInProgress() }?.let { listOf(it) } ?: emptyList()
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

  fun toMessage(task: PlayerMainTask): TaskInfo {
    return TaskInfo(task.id, task.status, getProgressList(task))
  }

  override val errorCode: TaskErrorCode
    get() = MainTaskErrorCode
  override val logSource: TaskLogSource
    get() = MainTaskLogSource

}
