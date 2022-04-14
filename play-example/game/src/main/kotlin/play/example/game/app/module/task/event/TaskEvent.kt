package play.example.game.app.module.task.event

import play.example.game.app.module.task.domain.TaskTargetType

/**
 * 任务事件
 *
 * @property type 对应的目标类型
 */
interface TaskEvent {
  val type: TaskTargetType
}

