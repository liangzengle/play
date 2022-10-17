package play.example.game.app.module.activity.impl.task.domain

import play.example.game.app.module.ModuleId
import play.example.game.app.module.task.domain.TaskLogSource

/**
 *
 *
 * @author LiangZengle
 */
object TaskActivityLogSource : TaskLogSource(ModuleId.ActivityTask) {
  override val TaskReward: Int = 1
}
