package play.example.game.app.module.task

import play.example.game.app.module.player.Self
import play.example.game.app.module.task.config.AbstractTaskResource
import play.example.game.app.module.task.entity.AbstractTask
import play.example.game.app.module.task.event.TaskEvent
import play.util.logging.getLogger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 任务事件接收器
 *
 * @author LiangZengle
 */
@Singleton
@Named
class TaskEventReceiver @Inject constructor(private val taskServices: List<AbstractTaskService<AbstractTask, AbstractTaskResource>>) {

  private val logger = getLogger()

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
