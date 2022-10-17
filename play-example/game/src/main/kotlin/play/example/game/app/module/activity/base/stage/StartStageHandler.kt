package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.util.time.Time

/**
 *
 * @author LiangZengle
 */
object StartStageHandler : ActivityStageHandler, ActivityStageHandler.Suspendable,
  ActivityStageHandler.EventTriggerable {

  context(ActivityActor)
    override fun start(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Init)

    if (serverConditionService.check(resource.startConditions).isErr()) {
      logger.info { "活动[${resource.id}]开启失败，条件不满足" }
      return
    }

    entity.stage = play.example.game.app.module.activity.base.stage.ActivityStage.Start
    entity.startTime = Time.currentMillis()
    entity.openTimes++

    logger.info { "活动[${resource.id}]开始了" }

    handler.onStageChanged(entity, resource)

    refresh(entity, resource)
  }

  context(ActivityActor)
    override fun refresh(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Start)

    if (entity.isSuspended()) {
      return
    }

    if (!triggerContext.isForeverOpen(resource.startTime)) {
      val endTime = entity.startTime + resource.duration.toMillis() + entity.suspendedMillis
      scheduleAt(endTime, ActivityActor.ActivityEnd)
      entity.suspendedMillis = 0
      logger.info { "活动[${resource.id}]将于[${Time.toLocalDateTime(endTime)}]结束" }
    }

    for ((key, trigger) in resource.eventTriggers) {
      val eventTriggerTime = trigger.nextTriggerTime(Time.currentDateTime(), triggerContext)
      if (eventTriggerTime != null) {
        scheduleAt(eventTriggerTime, ActivityActor.ActivityTriggerEvent(key))
      }
    }
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
