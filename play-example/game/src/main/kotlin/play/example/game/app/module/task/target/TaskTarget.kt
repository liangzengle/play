package play.example.game.app.module.task.target

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Min
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.json.TaskTargetTypeResolver
import play.util.json.JsonAbstractType

/**
 * 任务目标基类
 *
 * @property type 目标类型
 * @property value 目标值
 * @property includeRecord 是否包含历史值
 */
@JsonAbstractType(TaskTargetTypeResolver::class)
abstract class TaskTarget(
  val type: TaskTargetType,
  @field:Min(1)
  val value: Int = 1,
  @field:JsonFormat(shape = JsonFormat.Shape.NUMBER)
  val includeRecord: Boolean = false
)
