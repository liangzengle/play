package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.util.time.Time

/**
 *
 * @author LiangZengle
 */
interface ActivityStageHandler {

  val logger get() = ActivityActor.logger

  context(ActivityActor)
  fun start(entity: ActivityEntity, resource: ActivityResource)

  context(ActivityActor)
  fun refresh(entity: ActivityEntity, resource: ActivityResource)

  context(ActivityActor)
  fun reload(entity: ActivityEntity, resource: ActivityResource)

  interface Suspendable {
    context(ActivityActor)
    fun suspend(entity: ActivityEntity, resource: ActivityResource)

    context(ActivityActor)
    fun resume(entity: ActivityEntity, resource: ActivityResource)
  }

  interface EventTriggerable {
    context(ActivityActor)
    fun eventTriggered(eventName: String, entity: ActivityEntity, resource: ActivityResource) {
      val trigger = resource.eventTriggers[eventName] ?: return
      try {
        handler.onActivityEvent(eventName, entity, resource)
      } finally {
        val nextTriggerTime = trigger.nextTriggerTime(Time.currentDateTime(), triggerContext)
        if (nextTriggerTime != null) {
          scheduleAt(nextTriggerTime, ActivityActor.ActivityTriggerEvent(eventName))
        }
      }
    }
  }
}
