package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.util.time.Time

/**
 *
 * @author LiangZengle
 */
object EndStageHandler : ActivityStageHandler, ActivityStageHandler.Suspendable,
  ActivityStageHandler.EventTriggerable {

  context(ActivityActor)
    override fun start(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Start)

    cancelAllSchedules(ActivityActor.ActivityTriggerEvent::class.java)

    entity.stage = play.example.game.app.module.activity.base.stage.ActivityStage.End
    entity.endTime = Time.currentMillis()

    logger.info { "活动[${resource.id}]结束了" }

    handler.onStageChanged(entity, resource)

    refresh(entity, resource)
  }

  context(ActivityActor)
    override fun refresh(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.End)

    if (entity.isSuspended()) {
      return
    }

    val closeTime = entity.endTime + resource.closeDelay.toMillis() + entity.suspendedMillis
    scheduleAt(closeTime, ActivityActor.ActivityClose)
    entity.suspendedMillis = 0

    logger.info { "活动[${resource.id}]将于[${Time.toLocalDateTime(closeTime)}]关闭" }
  }

  context(ActivityActor)
    override fun suspend(entity: ActivityEntity, resource: ActivityResource) {
    val now = Time.currentMillis()
    entity.suspendedMillis += if (entity.suspendTime > 0) now - entity.suspendTime else 0
    entity.suspendTime = Time.currentMillis()

    cancelAllSchedules()
  }

  context(ActivityActor)
    override fun resume(entity: ActivityEntity, resource: ActivityResource) {
    val now = Time.currentMillis()
    entity.suspendedMillis += if (entity.suspendTime > 0) now - entity.suspendTime else 0
    entity.suspendTime = 0

    refresh(entity, resource)
  }

  context(ActivityActor)
    override fun reload(entity: ActivityEntity, resource: ActivityResource) {
    cancelAllSchedules()
    refresh(entity, resource)
  }
}
