package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.util.time.Time

/**
 *
 * @author LiangZengle
 */
object CloseStageHandler : ActivityStageHandler {

  context(ActivityActor)
    override fun start(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.End)

    entity.stage = play.example.game.app.module.activity.base.stage.ActivityStage.Close
    entity.closeTime = Time.currentMillis()

    logger.info { "活动[${resource.id}]关闭了" }

    handler.onStageChanged(entity, resource)

    refresh(entity, resource)
  }

  context(ActivityActor)
    override fun refresh(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Close)

    // reschedule for next round
    if (resource.openTimes == 0 || entity.openTimes < resource.openTimes) {
      val nextStartTime = resource.startTime.nextTriggerTime(Time.toLocalDateTime(entity.closeTime), triggerContext)
      if (nextStartTime != null) {
        InitStageHandler.start(entity, resource)
      }
    }
  }

  context(ActivityActor)
    override fun reload(entity: ActivityEntity, resource: ActivityResource) {
    refresh(entity, resource)
  }
}
