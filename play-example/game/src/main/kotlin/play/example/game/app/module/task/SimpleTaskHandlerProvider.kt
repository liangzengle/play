package play.example.game.app.module.task

import org.springframework.stereotype.Component
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.handler.SimpleTaskTargetHandler
import play.example.game.app.module.task.target.TaskTarget
import play.util.collection.toImmutableMap

/**
 *
 * @author LiangZengle
 */
@Component
class SimpleTaskHandlerProvider(handlers: List<SimpleTaskTargetHandler<TaskTarget, TaskEvent>>) {

  private val handlerMap = handlers.toImmutableMap { it.type }

  fun getHandlerOrNull(type: TaskTargetType) = handlerMap[type]
}
