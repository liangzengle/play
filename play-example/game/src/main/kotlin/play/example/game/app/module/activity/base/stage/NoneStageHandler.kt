package play.example.game.app.module.activity.base.stage

import play.example.game.app.module.activity.base.ActivityActor
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource

/**
 *
 * @author LiangZengle
 */
object NoneStageHandler : ActivityStageHandler {
  context(ActivityActor)
    override fun start(entity: ActivityEntity, resource: ActivityResource) {
    check(entity.stage == play.example.game.app.module.activity.base.stage.ActivityStage.None)
    refresh(entity, resource)
  }

  context(ActivityActor)
    override fun refresh(entity: ActivityEntity, resource: ActivityResource) {
    InitStageHandler.start(entity, resource)
  }

  context(ActivityActor)
    override fun reload(entity: ActivityEntity, resource: ActivityResource) {
  }
}
