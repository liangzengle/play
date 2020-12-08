package play.example.module.task.event

import play.example.module.task.domain.TaskTargetType

/**
 * 任务事件
 *
 * @property type 对应的目标类型
 */
abstract class TaskEvent(val type: TaskTargetType)
