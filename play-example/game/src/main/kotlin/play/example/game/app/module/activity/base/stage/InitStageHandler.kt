package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.util.time.Time
import play.util.time.Time.toMillis
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
object InitStageHandler : ActivityStageHandler {
  context(ActivityActor)
    override fun start(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.None || entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Close)
    if (serverConditionService.check(resource.initConditions).isErr()) {
      logger.info { "活动[${resource.id}]初始化失败，条件不满足" }
      return
    }
    if (resource.openTimes > 0 && resource.openTimes < entity.openTimes) {
      logger.info { "活动[${resource.id}]初始化失败，已经到达开启次数上限" }
      return
    }
    init(entity, resource)
  }

  context(ActivityActor)
    override fun refresh(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.Init)

    if (triggerContext.isForeverOpen(resource.startTime)) {
      return
    }

    val startTime = entity.startTime
    val noticeAhead = resource.noticeAhead
    if (noticeAhead > Duration.ZERO) {
      val noticeTime = startTime - noticeAhead.toMillis()
      val timeoutMillis = noticeTime - Time.currentMillis()
      if (timeoutMillis > 0) {
        scheduleWithTimeout(Duration.ofMillis(timeoutMillis), ActivityActor.ActivityNotice)
      }
    }
    scheduleAt(startTime, ActivityActor.ActivityStart)
    logger.info { "活动[${resource.id}]将于[${Time.toLocalDateTime(startTime)}]开启}" }
  }

  context(ActivityActor)
    override fun reload(entity: ActivityEntity, resource: ActivityResource) {
    cancelAllSchedules()
    init(entity, resource)
  }

  context(ActivityActor)
    private fun init(entity: ActivityEntity, resource: ActivityResource) {
    if (triggerContext.isForeverClose(resource.startTime)) {
      logger.info { "活动[${resource.id}]为永久关闭" }
      return
    }

    val startTime = resource.startTime.nextTriggerTime(Time.currentDateTime(), triggerContext)
    if (startTime == null) {
      logger.info { "活动[${resource.id}]无法计算开启时间" }
      return
    }

    val foreverOpen = triggerContext.isForeverOpen(resource.startTime)

    entity.stage = play.example.game.app.module.activity.base.stage.ActivityStage.Init
    entity.startTime = startTime.toMillis()


    logger.info { "活动初始化完成: activityId=${resource.id}, startTime=$startTime, foreverOpen=$foreverOpen" }

    if (foreverOpen) {
      StartStageHandler.start(entity, resource)
      logger.info { "活动[$${resource.id}]将永久开启" }
    } else {
      refresh(entity, resource)
    }
  }
}
