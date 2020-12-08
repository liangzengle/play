package play.example.module.maintask

import javax.inject.Inject
import javax.inject.Singleton
import play.example.module.maintask.config.MainTaskConfig
import play.example.module.maintask.config.MainTaskConfigSet
import play.example.module.maintask.domain.MainTaskErrorCode
import play.example.module.maintask.domain.MainTaskLogSource
import play.example.module.maintask.entity.MainTaskEntityCache
import play.example.module.maintask.entity.PlayerMainTask
import play.example.module.player.Self
import play.example.module.task.AbstractTaskService
import play.example.module.task.domain.TaskErrorCode
import play.example.module.task.domain.TaskLogSource
import play.example.module.task.event.TaskEvent
import play.example.module.task.message.TaskInfo
import play.util.filterOrNull

/**
 * 主线任务模块逻辑处理
 */
@Singleton
public class MainTaskService @Inject constructor(
  private val mainTaskEntityCache: MainTaskEntityCache
) : AbstractTaskService<PlayerMainTask, MainTaskConfig>() {

  /**
   * 添加任务
   *
   * @param self 自己
   * @param playerTask 新任务
   */
  override fun addPlayerTask(self: Self, playerTask: PlayerMainTask) {
    val entity = mainTaskEntityCache.getOrCreate(self.id)
    entity.task = playerTask
  }

  /**
   * 创建一个玩家任务对象
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return PlayerTask
   */
  override fun createPlayerTask(self: Self, taskConf: MainTaskConfig): PlayerMainTask {
    return PlayerMainTask(taskConf.id)
  }

  /**
   * 获取自己的某个任务
   *
   * @param self 自己
   * @param taskId 任务id
   * @return PlayerTask?
   */
  override fun getPlayerTask(self: Self, taskId: Int): PlayerMainTask? {
    return mainTaskEntityCache.getOrNull(self.id)?.task
  }

  /**
   * 检查任务是否接取
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return 状态码
   */
  override fun checkAcceptConditions(self: Self, taskConf: MainTaskConfig) = MainTaskErrorCode.Success

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
  override fun getTaskConfig(taskId: Int): MainTaskConfig? {
    return MainTaskConfigSet.getOrNull(taskId)
  }

  fun toMessage(task: PlayerMainTask): TaskInfo {
    return TaskInfo(task.id, task.status, getProgressList(task))
  }

  override val errorCode: TaskErrorCode
    get() = MainTaskErrorCode
  override val logSource: TaskLogSource
    get() = MainTaskLogSource

}
