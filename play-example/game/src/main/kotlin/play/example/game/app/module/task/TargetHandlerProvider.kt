package play.example.game.app.module.task

import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.event.TaskEvent
import play.example.game.app.module.task.handler.TaskTargetHandler
import play.example.game.app.module.task.target.TaskTarget
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.unsafeCastOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * TargetHandler提供者
 *
 * @author LiangZengle
 */
@Singleton
@Named
class TargetHandlerProvider @Inject constructor(val injector: PlayInjector) {

  private val targetHandlers by lazy(LazyThreadSafetyMode.NONE) {
    injector.getInstancesOfType(TaskTargetHandler::class).toImmutableMap { it.type }
  }

  fun getOrNull(type: TaskTargetType): TaskTargetHandler<TaskTarget, TaskEvent>? {
    return targetHandlers[type].unsafeCastOrNull()
  }
}
