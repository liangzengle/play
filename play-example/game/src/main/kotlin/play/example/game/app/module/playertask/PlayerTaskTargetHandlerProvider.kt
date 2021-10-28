package play.example.game.app.module.playertask

import org.springframework.stereotype.Component
import play.example.game.app.module.playertask.handler.PlayerTaskTargetHandler
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.target.TaskTarget
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.unsafeCastOrNull

/**
 * TargetHandler提供者
 *
 * @author LiangZengle
 */
@Component
class PlayerTaskTargetHandlerProvider(val injector: PlayInjector) {

  private val targetHandlers by lazy(LazyThreadSafetyMode.NONE) {
    injector.getInstancesOfType(PlayerTaskTargetHandler::class).toImmutableMap { it.type }
  }

  fun getOrNull(type: TaskTargetType): PlayerTaskTargetHandler<TaskTarget, TaskEvent>? {
    return targetHandlers[type].unsafeCastOrNull()
  }
}
