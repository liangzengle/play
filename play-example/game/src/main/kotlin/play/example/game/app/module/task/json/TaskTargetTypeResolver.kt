package play.example.game.app.module.task.json

import com.fasterxml.jackson.databind.JsonNode
import play.example.game.app.module.task.domain.TaskTargetTypes
import play.example.game.app.module.task.target.TaskTarget
import play.util.json.AbstractTypeResolver

class TaskTargetTypeResolver : AbstractTypeResolver<TaskTarget>() {
  override fun resolve(node: JsonNode): Class<out TaskTarget> {
    val targetType =  node.get("type")?.textValue()?.let {
      TaskTargetTypes.getByNameOrNull(it)
    } ?: throw IllegalArgumentException("错误的任务目标类型: $node")
    return targetType.taskTargetClass
  }

  override fun recover(ex: Throwable): TaskTarget? {
//    return ModeDependent.logAndRecover(ex, NonRawReward) { ex.message }
    return null
  }
}
