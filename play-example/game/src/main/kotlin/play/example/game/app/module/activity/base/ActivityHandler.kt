package play.example.game.app.module.activity.base

import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.stage.ActivityStage
import play.example.game.app.module.player.PlayerManager
import play.spring.BeanWithType

/**
 *
 * @author LiangZengle
 */
interface ActivityHandler : BeanWithType<ActivityType> {
  val logger get() = ActivityActor.logger

  /** 活动阶段变化 */
  fun onStageChanged(entity: ActivityEntity, resource: ActivityResource) {
    try {
      when (entity.stage) {
        ActivityStage.None -> {}
        ActivityStage.Init -> {}
        ActivityStage.Start -> onStart(entity, resource)
        ActivityStage.End -> onEnd(entity, resource)
        ActivityStage.Close -> {
          onClose(entity, resource)
          afterClose(entity, resource)
        }
      }
    } catch (e: Exception) {
      ActivityActor.logger.error(e) { "活动[${resource.id}]处理报错" }
    }
  }

  fun onActivityEvent(name: String, entity: ActivityEntity, resource: ActivityResource) {}

  fun onNotice(resource: ActivityResource) {}

  fun onStart(entity: ActivityEntity, resource: ActivityResource) {}

  fun onEnd(entity: ActivityEntity, resource: ActivityResource) {}

  fun onClose(entity: ActivityEntity, resource: ActivityResource) {
  }

  fun afterClose(entity: ActivityEntity, resource: ActivityResource) {
    entity.clearData()
  }

  fun join(
    self: PlayerManager.Self,
    playerActivityEntity: PlayerActivityEntity,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
  }
}
