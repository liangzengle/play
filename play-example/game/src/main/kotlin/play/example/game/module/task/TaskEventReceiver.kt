package play.example.game.module.task

import javax.inject.Inject
import javax.inject.Singleton
import play.example.game.module.player.Self
import play.example.game.module.task.config.AbstractTaskConfig
import play.example.game.module.task.entity.AbstractTask
import play.example.game.module.task.event.TaskEvent
import play.util.logging.getLogger

/**
 * 任务事件接收器
 *
 * @author LiangZengle
 */
@Singleton
class TaskEventReceiver @Inject constructor(private val taskServices: List<AbstractTaskService<AbstractTask, AbstractTaskConfig>>) {

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
