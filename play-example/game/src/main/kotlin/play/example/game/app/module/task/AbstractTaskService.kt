package play.example.game.app.module.task

import play.example.common.StatusCode
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.entity.AbstractTask
import play.example.game.app.module.task.entity.TaskStatus
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.event.adpater.TaskEventAdapters
import play.example.game.app.module.task.handler.DomainTaskTargetHandler
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.res.AbstractTaskResourceExtension
import play.example.game.app.module.task.target.TaskTarget
import play.util.control.Result2
import play.util.control.ok
import play.util.logging.getLogger
import play.util.max
import java.util.*

/**
 * 任务模块服务
 *
 * @author LiangZengle
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractTaskService<T, Task : AbstractTask, TaskConfig : AbstractTaskResource>(
  protected val commonTaskHandlerProvider: CommonTaskHandlerProvider
) {

  protected val logger = getLogger()

  /**
   * 获取目标处理器
   *
   * @param targetType 任务目标类型
   */
  protected abstract fun getHandlerOrNull(targetType: TaskTargetType): DomainTaskTargetHandler<T, TaskTarget, TaskEvent>?

  protected abstract fun getResourceExtension(): AbstractTaskResourceExtension<TaskConfig>?

  private fun isInterested(targetType: TaskTargetType): Boolean {
    val extension = getResourceExtension() ?: return true
    return extension.containsTargetType(targetType)
  }

  /**
   * 处理任务事件
   *
   * @param owner 任务归属者
   * @param taskEvent 任务事件
   */
  fun onEvent(owner: T, taskEvent: TaskEvent) {
    if (!isInterested(taskEvent.type)) {
      return
    }
    val commonHandler = commonTaskHandlerProvider.getHandlerOrNull(taskEvent.type)
    val domainHandler = getHandlerOrNull(taskEvent.type)
    if (commonHandler == null && domainHandler == null) {
      logger.error { "找不到任务目标处理器：${taskEvent.type}" }
      return
    }
    val tasks = getTasksInProgress(owner, taskEvent)
    val changedTasks = LinkedList<Task>()
    for (task in tasks) {
      if (task.status != TaskStatus.Accepted) {
        continue
      }
      val taskConf = getTaskConfig(task.id)
      if (taskConf == null) {
        logger.error { "找不到任务配置: ${task.javaClass.simpleName}(${task.id})" }
        continue
      }
      var changed = false
      for (i in taskConf.targets.indices) {
        val target = getTarget(owner, taskConf, i)
        if (target.type != taskEvent.type) {
          continue
        }
        val progress = task.getProgress(i)
        val change = domainHandler?.onEvent(owner, target, taskEvent, progress, taskConf)
          ?: commonHandler?.onEvent(target, taskEvent, progress, taskConf) ?: 0
        if (change == 0) {
          continue
        }
        var newProgress = (progress + change) max 0
        if (newProgress > target.value && trimProgress(taskConf)) {
          newProgress = target.value
        }
        task.setProgress(i, newProgress)
        onTaskProgressChanged(owner, task, target, i)
        changed = true
      }
      if (changed) {
        trySetTaskFinished(owner, task, taskConf)
        changedTasks += task
      }
    }
    if (changedTasks.isNotEmpty()) {
      onTaskChanged(owner, changedTasks)
    }

    val adaptedEvents = TaskEventAdapters.adapt(taskEvent)
    if (adaptedEvents.isNotEmpty()) {
      for (adaptedEvent in adaptedEvents) {
        onEvent(owner, adaptedEvent)
      }
    }
  }

  /**
   * 任务接取
   *
   * @param owner 任务归属者
   * @param taskId 任务id
   * @return 任务接取结果
   */
  open fun acceptTask(owner: T, taskId: Int): Result2<Task> {
    val taskConfig = getTaskConfig(taskId) ?: return StatusCode.ResourceNotFound
    val checkResult = checkAcceptConditions(owner, taskConfig)
    if (checkResult != errorCode.Success) {
      return checkResult
    }
    val acceptedTask = getTask(owner, taskId)
    if (acceptedTask != null) {
      return errorCode.Failure
    }
    val newTask = createTask(owner, taskConfig)
    addTask(owner, newTask)
    val changed = initTaskProgresses(owner, newTask, taskConfig)
    if (changed && isProgressFinished(newTask, taskConfig)) {
      trySetTaskFinished(owner, newTask, taskConfig)
    }
    return ok(newTask)
  }

  /**
   * 添加任务
   *
   * @param owner 任务归属者
   * @param task 新任务
   */
  abstract fun addTask(owner: T, task: Task)

  /**
   * 初始化任务进度
   *
   * @param owner 任务归属者
   * @param task 任务
   * @param taskConf 任务配置
   * @return 进度是否有变化
   */
  protected fun initTaskProgresses(owner: T, task: Task, taskConf: TaskConfig): Boolean {
    val targets = taskConf.targets
    var changed = false
    for (i in targets.indices) {
      val target = targets[i]
      val handler = getHandlerOrNull(target.type) ?: continue
      val initialProgress = handler.getInitialProgress(owner, target, taskConf)
      if (initialProgress > 0) {
        task.setProgress(i, initialProgress)
        changed = true
      }
    }
    return changed
  }

  /**
   * 创建一个玩家任务对象
   *
   * @param owner 任务归属者
   * @param taskConf 任务配置
   * @return PlayerTask
   */
  abstract fun createTask(owner: T, taskConf: TaskConfig): Task

  /**
   * 获取自己的某个任务
   *
   * @param owner 任务归属者
   * @param taskId 任务id
   * @return PlayerTask?
   */
  abstract fun getTask(owner: T, taskId: Int): Task?

  /**
   * 检查任务是否接取
   *
   * @param owner 任务归属者
   * @param taskConf 任务配置
   * @return 状态码
   */
  abstract fun checkAcceptConditions(owner: T, taskConf: TaskConfig): Result2<Nothing>

  /**
   * 任务进度发生变化时
   *
   * @param owner 任务归属者
   * @param changedTasks 发生变化的任务
   */
  abstract fun onTaskChanged(owner: T, changedTasks: List<Task>)

  /**
   * 进度变化时，尝试将任务设置为完成状态
   *
   * @param owner 任务归属者
   * @param playerTask 任务
   * @param taskConf 任务配置
   * @return 任务是否完成
   */
  protected open fun trySetTaskFinished(owner: T, playerTask: Task, taskConf: TaskConfig): Boolean {
    if (!isProgressFinished(playerTask, taskConf)) {
      return false
    }
    playerTask.setFinished()
    onTaskFinished(owner, playerTask, taskConf)
    return true
  }

  /**
   * 任务状态变为完成时
   *
   * @param owner 任务归属者
   * @param task 任务
   * @param taskConf 任务配置
   */
  protected open fun onTaskFinished(owner: T, task: Task, taskConf: TaskConfig) {
  }

  protected open fun onTaskRewarded(owner: T, task: Task, taskConf: TaskConfig) {
  }

  /**
   * 根据任务进度判断任务是否已完成
   *
   * @param task 任务
   * @param taskConf 任务配置
   * @return true-是
   */
  protected fun isProgressFinished(task: Task, taskConf: TaskConfig): Boolean {
    for (i in taskConf.targets.indices) {
      if (taskConf.targets[i].value > task.getProgress(i)) {
        return false
      }
    }
    return true
  }

  /**
   * 任务进度发生变化时
   *
   * @param owner 任务归属者
   * @param task 任务
   * @param target 任务目标
   * @param targetIndex 任务目标索引
   */
  protected open fun onTaskProgressChanged(owner: T, task: Task, target: TaskTarget, targetIndex: Int) {

  }

  /**
   * 任务进度是否允许超出任务目标值
   *
   * @return 默认不允许
   */
  protected open fun trimProgress(taskConf: TaskConfig) = true

  /**
   * 获取玩家进行中的任务
   *
   * @param owner 任务归属者
   * @param event 当前触发的任务事件
   * @return 进行中的任务
   */
  protected abstract fun getTasksInProgress(owner: T, event: TaskEvent): Collection<Task>

  /**
   * 获取任务配置
   *
   * @param taskId 任务id
   * @return 任务配置 or null
   */
  abstract fun getTaskConfig(taskId: Int): TaskConfig?

  /**
   * 获取任务目标
   *
   * @param owner  任务归属者
   * @param taskConf 任务配置
   * @param targetIndex 目标索引
   * @return 任务目标
   */
  protected open fun getTarget(owner: T, taskConf: TaskConfig, targetIndex: Int): TaskTarget {
    return taskConf.targets[targetIndex]
  }

  /**
   * 获取任务的奖励配置
   *
   * @param owner 任务归属者
   * @param taskConf 任务配置
   * @return 任务奖励
   */
  protected open fun getRewards(owner: T, taskConf: TaskConfig): RewardList {
    return taskConf.rewards
  }

  /**
   * 取列表形式的任务进度表示
   *
   * @param task 玩家任务
   * @return List<Int>
   */
  protected fun getProgressList(task: Task): List<Int> {
    val taskConfig = getTaskConfig(task.id)!!
    if (taskConfig.targets.size <= task.progresses.size) {
      return task.progresses.asList()
    }
    return task.progresses.copyOf(taskConfig.targets.size).asList()
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
