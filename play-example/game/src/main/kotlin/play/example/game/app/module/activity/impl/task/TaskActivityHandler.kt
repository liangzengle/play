package play.example.game.app.module.activity.impl.task

import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityTaskEventHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.impl.task.res.TaskActivityResourceSet
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerTaskEventLike
import play.example.game.app.module.playertask.PlayerTaskTargetHandlerProvider
import play.example.game.app.module.reward.RewardService

/**
 *
 *
 * @author LiangZengle
 */
@Component
class TaskActivityHandler(
  private val targetHandlerProvider: PlayerTaskTargetHandlerProvider,
  private val rewardService: RewardService
) : ActivityHandler, ActivityTaskEventHandler {

  override fun type(): ActivityType = ActivityType.TASK

  override fun join(
    self: PlayerManager.Self,
    playerActivityEntity: PlayerActivityEntity,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    super.join(self, playerActivityEntity, activityEntity, resource)

  }

  override fun onTaskEvent(
    self: PlayerManager.Self,
    event: PlayerTaskEventLike,
    playerActivityEntity: PlayerActivityEntity,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    if (!TaskActivityResourceSet.extension().containsTargetType(event.taskEvent.targetType)) {
      return
    }
    TaskActivityTaskService(playerActivityEntity, targetHandlerProvider, rewardService)
      .onEvent(self, event.taskEvent)
  }
}
