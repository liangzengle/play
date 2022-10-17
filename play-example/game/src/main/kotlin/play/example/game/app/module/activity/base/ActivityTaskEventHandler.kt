package play.example.game.app.module.activity.base

import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerTaskEventLike

/**
 *
 *
 * @author LiangZengle
 */
interface ActivityTaskEventHandler {
  fun onTaskEvent(
    self: PlayerManager.Self,
    event: PlayerTaskEventLike,
    playerActivityEntity: PlayerActivityEntity,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  )
}
