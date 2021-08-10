package play.example.game.app.module.task.target.json

import com.fasterxml.jackson.databind.node.ObjectNode
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.TaskTarget
import play.util.json.AbstractTypeResolver

class TaskTargetAbstractTypeResolver : AbstractTypeResolver<TaskTarget>() {
  override fun resolve(node: ObjectNode): Class<out TaskTarget> {
    val typeNode = node.get("type")
    val targetType = TaskTargetType.valueOf(typeNode.textValue())
    if (targetType == TaskTargetType.None) {
      throw IllegalStateException("错误的奖励类型(Reward): $node")
    }
    return targetType.taskTargetClass
  }

  override fun recover(ex: Throwable): TaskTarget? {
//    return ModeDependent.logAndRecover(ex, NonRawReward) { ex.message }
    return null
  }
}
