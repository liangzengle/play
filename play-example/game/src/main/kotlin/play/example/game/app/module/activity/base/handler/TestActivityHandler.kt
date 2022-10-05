package play.example.game.app.module.activity.base.handler

import org.springframework.stereotype.Component
import play.example.game.app.module.activity.base.ActivityHandler
import play.example.game.app.module.activity.base.ActivityType
import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource

/**
 *
 * @author LiangZengle
 */
@Component
class TestActivityHandler : ActivityHandler {
  override fun onActivityEvent(name: String, entity: ActivityEntity, resource: ActivityResource) {
    logger.debug { "activity(${resource.id}) trigger event: $name" }
  }

  override fun onNotice(resource: ActivityResource) {
    logger.debug { "activity(${resource.id}) onNotice" }
  }

  override fun onStart(entity: ActivityEntity, resource: ActivityResource) {
    logger.debug { "activity(${resource.id}) onStart" }
  }

  override fun onEnd(entity: ActivityEntity, resource: ActivityResource) {
    logger.debug { "activity(${resource.id}) onEnd" }
  }

  override fun onClose(entity: ActivityEntity, resource: ActivityResource) {
    logger.debug { "activity(${resource.id}) onClose" }
  }

  override fun type(): ActivityType = ActivityType.TEST
}
