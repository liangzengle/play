package play.example.game.app.module.task

import org.springframework.stereotype.Component
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.handler.CommonTaskTargetHandler
import play.example.game.app.module.task.target.TaskTarget
import play.util.collection.toImmutableMap

/**
 *
 * @author LiangZengle
 */
@Component
class CommonTaskHandlerProvider(handlers: List<CommonTaskTargetHandler<TaskTarget, TaskEvent>>) {

  private val handlerMap = handlers.toImmutableMap { it.type }

  fun getHandlerOrNull(type: TaskTargetType) = handlerMap[type]
}
