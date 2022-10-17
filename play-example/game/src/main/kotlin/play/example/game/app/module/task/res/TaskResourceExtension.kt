package play.example.game.app.module.task.res

import play.example.game.app.module.task.domain.TaskTargetType
import play.res.ResourceExtension
import play.util.collection.toImmutableSet

open class TaskResourceExtension<T : AbstractTaskResource>(elements: List<T>) : ResourceExtension<T>(elements) {
  val targetTypes = elements.asSequence().flatMap { it.targets }.map { it.type }.toImmutableSet()

  fun containsTargetType(targetType: TaskTargetType) = targetTypes.contains(targetType)
}
