package play.example.game.app.module.servertask

import com.google.common.collect.Collections2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.servertask.domain.ServerTaskTargetType
import play.example.game.app.module.servertask.entity.ServerTaskEntity
import play.example.game.app.module.servertask.entity.ServerTaskEntityCache
import play.example.game.app.module.servertask.event.ServerTaskEvent
import play.example.game.app.module.servertask.handler.ServerTaskTargetHandler
import play.example.game.app.module.servertask.res.ServerTaskResource
import play.example.game.app.module.servertask.res.ServerTaskResourceSet
import play.example.game.app.module.task.AbstractTaskService
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.entity.TaskData
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.res.TaskResourceExtension
import play.example.game.app.module.task.target.TaskTarget
import play.example.game.container.gs.domain.GameServerId
import play.inject.PlayInjector
import play.spring.OrderedSmartInitializingSingleton
import play.util.collection.toImmutableEnumMap
import play.util.control.Result2
import play.util.control.ok
import play.util.exists
import play.util.unsafeCastOrNull
import java.util.*

/**
 * 全服任务模块逻辑处理
 */
@Component
class ServerTaskService @Autowired constructor(
  private val serverTaskEntityCache: ServerTaskEntityCache,
  private val serverId: GameServerId,
  private val injector: PlayInjector
) :
  AbstractTaskService<ServerTaskEntity, TaskData, ServerTaskResource, ServerTaskEvent>(),
  OrderedSmartInitializingSingleton {

  private lateinit var handlerMap: Map<ServerTaskTargetType, ServerTaskTargetHandler<*, *>>

  override fun afterSingletonsInstantiated() {
    handlerMap = injector.getInstancesOfType(ServerTaskTargetHandler::class).toImmutableEnumMap { it.targetType() }
    serverTaskEntityCache.getOrCreate(serverId.toInt())
    checkNewTask()
  }

  override fun getHandlerOrNull(targetType: TaskTargetType): ServerTaskTargetHandler<TaskTarget, ServerTaskEvent>? {
    if (targetType !is ServerTaskTargetType) return null
    return handlerMap[targetType].unsafeCastOrNull()
  }

  override fun getResourceExtension(): TaskResourceExtension<ServerTaskResource>? {
    return ServerTaskResourceSet.extension()
  }

  override fun addTask(owner: ServerTaskEntity, task: TaskData) {
    owner.tasks.put(task.id, task)
  }

  override fun createTask(owner: ServerTaskEntity, taskConf: ServerTaskResource): TaskData {
    return TaskData(taskConf.id)
  }

  override fun getTask(owner: ServerTaskEntity, taskId: Int): TaskData? {
    return owner.tasks[taskId]
  }

  override fun checkAcceptConditions(owner: ServerTaskEntity, taskConf: ServerTaskResource): Result2<Nothing> {
    return ok()
  }

  override fun onTaskChanged(owner: ServerTaskEntity, changedTasks: List<TaskData>) {
  }

  override fun getTasksInProgress(owner: ServerTaskEntity, event: TaskEvent): Collection<TaskData> {
    return Collections2.filter(owner.tasks.values()) { it.isInProgress() }
  }

  override fun getTaskConfig(taskId: Int): ServerTaskResource? {
    return ServerTaskResourceSet.getOrNull(taskId)
  }

  /**
   * 检测新的任务
   */
  fun checkNewTask() {
    for (entity in serverTaskEntityCache.getAll()) {
      for (serverTaskResource in ServerTaskResourceSet.list()) {
        if (!entity.tasks.containsKey(serverTaskResource.id)) {
          acceptTask(entity, serverTaskResource.id)
        }
      }
    }
  }

  /**
   * 检测由于配置变化导致的任务完成
   */
  fun checkShouldFinished() {
    for (entity in serverTaskEntityCache.getAll()) {
      val changed = LinkedList<TaskData>()
      for (serverTask in entity.tasks.values()) {
        if (!serverTask.isInProgress()) {
          continue
        }
        val taskCfg = ServerTaskResourceSet.getOrNull(serverTask.id) ?: continue
        val finished = trySetTaskFinished(entity, serverTask, taskCfg)
        if (finished) {
          changed.add(serverTask)
        }
      }
      if (changed.isNotEmpty()) {
        onTaskChanged(entity, changed)
      }
    }
  }

  fun onEvent(event: ServerTaskEvent) {
    for (entity in serverTaskEntityCache.getAll()) {
      onEvent(entity, event)
    }
  }

  fun isTaskFinished(taskId: Int): Boolean {
    return serverTaskEntityCache.getCached(serverId.toInt())
      .exists { getTask(it, taskId)?.isFinished() ?: false }
  }

  fun getRewards(taskId: Int): RewardList {
    return getRewards(ServerTaskResourceSet.getOrThrow(taskId))
  }

  override fun getRewards(owner: ServerTaskEntity, taskConf: ServerTaskResource): RewardList {
    return getRewards(taskConf)
  }

  private fun getRewards(taskConf: ServerTaskResource): RewardList {
    return taskConf.rewards
  }

  override fun onTaskFinished(owner: ServerTaskEntity, task: TaskData, taskConf: ServerTaskResource) {
    super.onTaskFinished(owner, task, taskConf)
    logger.debug { "全服任务完成: $taskConf" }
  }
}
