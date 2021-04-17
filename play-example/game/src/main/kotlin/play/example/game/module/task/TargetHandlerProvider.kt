package play.example.game.module.task

import play.example.game.module.task.domain.TaskTargetType
import play.example.game.module.task.event.TaskEvent
import play.example.game.module.task.handler.TaskTargetHandler
import play.example.game.module.task.target.TaskTarget
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.unsafeCastOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TargetHandler提供者
 *
 * @author LiangZengle
 */
@Singleton
class TargetHandlerProvider @Inject constructor(val injector: PlayInjector) {

  private val targetHandlers by lazy(LazyThreadSafetyMode.NONE) {
    injector.getInstancesOfType(TaskTargetHandler::class).toImmutableMap { it.type }
  }

  fun getOrNull(type: TaskTargetType): TaskTargetHandler<TaskTarget, TaskEvent>? {
    return targetHandlers[type].unsafeCastOrNull()
  }
}
