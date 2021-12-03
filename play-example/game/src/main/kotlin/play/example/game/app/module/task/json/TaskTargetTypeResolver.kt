package play.example.game.app.module.task.json

import com.fasterxml.jackson.databind.JsonNode
import play.example.game.app.module.task.domain.CommonTaskTargetType
import play.example.game.app.module.task.domain.TaskTargetTypes
import play.example.game.app.module.task.target.TaskTarget
import play.util.json.AbstractTypeResolver

class TaskTargetTypeResolver : AbstractTypeResolver<TaskTarget>() {
  override fun resolve(node: JsonNode): Class<out TaskTarget> {
    val typeNode = node.get("type")
    val targetType = TaskTargetTypes.valueOf(typeNode.textValue())
    if (targetType == CommonTaskTargetType.None) {
      throw IllegalStateException("错误的奖励类型(Reward): $node")
    }
    return targetType.taskTargetClass
  }

  override fun recover(ex: Throwable): TaskTarget? {
//    return ModeDependent.logAndRecover(ex, NonRawReward) { ex.message }
    return null
  }
}
