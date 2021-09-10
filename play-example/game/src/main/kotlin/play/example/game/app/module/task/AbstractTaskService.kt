package play.example.game.app.module.task

import play.example.common.StatusCode
import play.example.game.app.module.player.Self
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.reward.model.RewardResultSet
import play.example.game.app.module.task.config.AbstractTaskResource
import play.example.game.app.module.task.domain.TaskErrorCode
import play.example.game.app.module.task.domain.TaskLogSource
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.entity.AbstractTask
import play.example.game.app.module.task.entity.TaskStatus
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.target.TaskTarget
import play.util.control.Result2
import play.util.control.ok
import play.util.logging.getLogger
import play.util.max
import java.util.*
import javax.inject.Inject

/**
 * 任务模块服务
 *
 * @author LiangZengle
 */
abstract class AbstractTaskService<PlayerTask : AbstractTask, TaskConfig : AbstractTaskResource> {

  @Inject
  protected lateinit var targetHandlerProvider: TargetHandlerProvider

  @Inject
  protected lateinit var rewardService: RewardService

  protected val logger = getLogger()

  /**
   * 获取目标处理器
   *
   * @param targetType 任务目标类型
   */
  protected fun getHandlerOrNull(targetType: TaskTargetType) = targetHandlerProvider.getOrNull(targetType)

  /**
   * 处理任务事件
   *
   * @param self 自己
   * @param taskEvent 任务事件
   */
  fun onEvent(self: Self, taskEvent: TaskEvent) {
    val targetHandler = getHandlerOrNull(taskEvent.type)
    if (targetHandler == null) {
      logger.error { "找不到任务目标处理器：${taskEvent.type}" }
      return
    }
    val tasks = getTasksInProgress(self, taskEvent)
    val changedTasks = LinkedList<PlayerTask>()
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
        val target = taskConf.targets[i]
        if (target.type != taskEvent.type) {
          continue
        }
        val progress = task.getProgress(i)
        val change = targetHandler.onEvent(self, target, taskEvent, progress, taskConf)
        if (change == 0) {
          continue
        }
        var newProgress = (progress + change) max 0
        if (newProgress > target.value && trimProgress()) {
          newProgress = target.value
        }
        task.setProgress(i, newProgress)
        onTaskProgressChanged(self, task, target, i)
        changed = true
      }
      if (changed) {
        trySetTaskFinished(self, task, taskConf)
        changedTasks += task
      }
    }
    if (changedTasks.isNotEmpty()) {
      onTaskChanged(self, changedTasks)
    }
  }

  /**
   * 任务接取
   *
   * @param self 自己
   * @param taskId 任务id
   * @return 任务接取结果
   */
  open fun acceptTask(self: Self, taskId: Int): Result2<PlayerTask> {
    val taskConfig = getTaskConfig(taskId) ?: return StatusCode.ConfigNotFound
    val checkResult = checkAcceptConditions(self, taskConfig)
    if (checkResult != errorCode.Success) {
      return checkResult
    }
    val playerTask = getPlayerTask(self, taskId)
    if (playerTask != null) {
      return errorCode.Failure
    }
    val pt = createPlayerTask(self, taskConfig)
    addPlayerTask(self, pt)
    val changed = initTaskProgresses(self, pt, taskConfig)
    if (changed && checkIfFinished(pt, taskConfig)) {
      trySetTaskFinished(self, pt, taskConfig)
    }
    return ok(pt)
  }

  /**
   * 领取任务奖励
   *
   * @param self 自己
   * @param taskId 任务id
   * @return 奖励结果
   */
  open fun getTaskReward(self: Self, taskId: Int): Result2<RewardResultSet> {
    val taskConfig = getTaskConfig(taskId) ?: return errorCode.ConfigNotFound
    val playerTask = getPlayerTask(self, taskId) ?: return errorCode.PlayerTaskNotExist
    if (playerTask.isRewarded()) {
      return errorCode.TaskRewarded
    }
    if (!playerTask.isFinished()) {
      return errorCode.TaskNotFinished
    }
    val rewards = getRewards(self, taskConfig)
    val rewardResult = rewardService.tryAndExecReward(self, RewardList(rewards), logSource.TaskReward)
    if (rewardResult.isOk()) {
      playerTask.setRewarded()
      onTaskRewarded(self, playerTask, taskConfig)
    }
    return rewardResult
  }

  /**
   * 添加任务
   *
   * @param self 自己
   * @param playerTask 新任务
   */
  abstract fun addPlayerTask(self: Self, playerTask: PlayerTask)

  /**
   * 初始化任务进度
   *
   * @param self 自己
   * @param task 任务
   * @param taskConf 任务配置
   * @return 进度是否有变化
   */
  protected fun initTaskProgresses(self: Self, task: PlayerTask, taskConf: TaskConfig): Boolean {
    val targets = taskConf.targets
    var changed = false
    for (i in targets.indices) {
      val target = targets[i]
      val handler = getHandlerOrNull(target.type) ?: continue
      val initialProgress = handler.getInitialProgress(self, target, taskConf)
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
   * @param self 自己
   * @param taskConf 任务配置
   * @return PlayerTask
   */
  abstract fun createPlayerTask(self: Self, taskConf: TaskConfig): PlayerTask

  /**
   * 获取自己的某个任务
   *
   * @param self 自己
   * @param taskId 任务id
   * @return PlayerTask?
   */
  abstract fun getPlayerTask(self: Self, taskId: Int): PlayerTask?

  /**
   * 检查任务是否接取
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return 状态码
   */
  abstract fun checkAcceptConditions(self: Self, taskConf: TaskConfig): Result2<Nothing>

  /**
   * 任务进度发生变化时
   *
   * @param self 自己
   * @param changedTasks 发生变化的任务
   */
  abstract fun onTaskChanged(self: Self, changedTasks: List<PlayerTask>)

  /**
   * 进度变化时，尝试将任务设置为完成状态
   *
   * @param self 自己
   * @param playerTask 任务
   * @param taskConf 任务配置
   * @return 任务是否完成
   */
  protected open fun trySetTaskFinished(self: Self, playerTask: PlayerTask, taskConf: TaskConfig): Boolean {
    if (!checkIfFinished(playerTask, taskConf)) {
      return false
    }
    playerTask.setFinished()
    onTaskFinished(self, playerTask, taskConf)
    return true
  }

  /**
   * 任务状态变为完成时
   *
   * @param self 自己
   * @param task 任务
   * @param taskConf 任务配置
   */
  protected open fun onTaskFinished(self: Self, task: PlayerTask, taskConf: TaskConfig) {
  }

  protected open fun onTaskRewarded(self: Self, task: PlayerTask, taskConf: TaskConfig) {

  }

  /**
   * 根据任务进度判断任务是否已完成
   *
   * @param task 任务
   * @param taskConf 任务配置
   * @return true-是
   */
  protected fun checkIfFinished(task: PlayerTask, taskConf: TaskConfig): Boolean {
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
   * @param self 自己
   * @param task 任务
   * @param target 任务目标
   * @param targetIndex 任务目标索引
   */
  protected open fun onTaskProgressChanged(self: Self, task: PlayerTask, target: TaskTarget, targetIndex: Int) {

  }

  /**
   * 任务进度是否允许超出任务目标值
   *
   * @return 默认为true
   */
  protected open fun trimProgress() = true

  /**
   * 获取玩家进行中的任务
   *
   * @param self 自己
   * @param event 当前触发的任务事件
   * @return 进行中的任务
   */
  protected abstract fun getTasksInProgress(self: Self, event: TaskEvent): Collection<PlayerTask>

  /**
   * 获取任务配置
   *
   * @param taskId 任务id
   * @return 任务配置 or null
   */
  abstract fun getTaskConfig(taskId: Int): TaskConfig?

  /**
   * 获取任务的奖励配置
   *
   * @param self 自己
   * @param taskConf 任务配置
   * @return 任务奖励
   */
  protected open fun getRewards(self: Self, taskConf: TaskConfig): List<Reward> {
    return taskConf.rewards;
  }

  /**
   * 取列表形式的任务进度表示
   *
   * @param task 玩家任务
   * @return List<Int>
   */
  protected fun getProgressList(task: PlayerTask): List<Int> {
    val taskConfig = getTaskConfig(task.id)!!
    val array = IntArray(taskConfig.targets.size)
    for (i in taskConfig.targets.indices) {
      array[i] = task.getProgress(i)
    }
    return array.asList()
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
