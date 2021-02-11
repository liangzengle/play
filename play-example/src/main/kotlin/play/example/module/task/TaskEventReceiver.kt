package play.example.module.task

import javax.inject.Inject
import javax.inject.Singleton
import play.example.module.player.Self
import play.example.module.task.event.TaskEvent
import play.getLogger
import play.inject.PlayInjector
import play.util.unsafeLazy

/**
 * 任务事件接收器
 *
 * @author LiangZengle
 */
@Singleton
class TaskEventReceiver @Inject constructor(val injector: PlayInjector) {

  private val logger = getLogger()

  private val taskServices by unsafeLazy {
    injector.getInstancesOfType(AbstractTaskService::class)
  }

  fun receive(self: Self, event: TaskEvent) {
    val taskService = this.taskServices
    for (i in taskService.indices) {
      val service = taskService[i]
      try {
        service.onEvent(self, event)
      } catch (e: Exception) {
        logger.error(e) { "任务事件处理失败: $event" }
      }
    }
  }
}
