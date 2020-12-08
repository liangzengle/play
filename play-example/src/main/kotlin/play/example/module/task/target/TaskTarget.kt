package play.example.module.task.target

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import play.ModeDependent
import play.example.module.task.domain.TaskTargetType
import play.util.json.Json
import javax.validation.constraints.Min

/**
 * 任务目标基类
 * @author LiangZengle
 */
abstract class TaskTarget(val type: TaskTargetType, @field:Min(1) val value: Int = 1) {

  companion object {
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): TaskTarget {
      return try {
        val type = node.get("type")
        val targetType = TaskTargetType.valueOf(type.asText())
        Json.convert(node, targetType.taskTargetClass)
      } catch (e: Exception) {
        ModeDependent.logAndRecover(e, NonTarget) { "任务目标解析失败: $node" }
      }
    }
  }
}
