package play.example.game.app.module.task.target

import com.fasterxml.jackson.annotation.JsonFormat
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.json.TaskTargetTypesResolver
import play.util.json.JsonAbstractType
import javax.validation.constraints.Min

/**
 * 任务目标基类
 *
 * @property type 目标类型
 * @property value 目标值
 * @property includeRecord 是否包含历史值
 */
@JsonAbstractType(TaskTargetTypesResolver::class)
abstract class TaskTarget(
  val type: TaskTargetType,
  @field:Min(1)
  val value: Int = 1,
  @field:JsonFormat(shape = JsonFormat.Shape.NUMBER)
  val includeRecord: Boolean = false
)
