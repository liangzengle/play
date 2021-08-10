package play.example.game.app.module.task.target

import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.json.TaskTargetAbstractTypeResolver
import play.util.json.JsonAbstractType
import javax.validation.constraints.Min

/**
 * 任务目标基类
 * @author LiangZengle
 */
@JsonAbstractType(TaskTargetAbstractTypeResolver::class)
abstract class TaskTarget(val type: TaskTargetType, @field:Min(1) val value: Int = 1)
