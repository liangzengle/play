package play.example.game.app.module.task.event

import play.example.game.app.module.task.domain.TaskTargetType

/**
 * 任务事件
 *
 * @property targetType 对应的目标类型
 */
interface TaskEvent {
  val targetType: TaskTargetType
}

