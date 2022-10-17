package play.example.game.app.module.activity.impl.task.res

import jakarta.validation.constraints.Positive
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.res.TaskResourceExtension
import play.res.ExtensionKey
import play.res.Grouped
import play.res.validation.constraints.ReferTo
import java.util.*

/**
 *
 *
 * @author LiangZengle
 */
class TaskActivityResource : AbstractTaskResource(), Grouped<Int>, ExtensionKey<TaskActivityResourceExtension> {

  @ReferTo(ActivityResource::class)
  @Positive
  val activityId = 0

  override fun groupBy(): Int {
    return activityId
  }
}

class TaskActivityResourceExtension(list: List<TaskActivityResource>) :
  TaskResourceExtension<TaskActivityResource>(list)
