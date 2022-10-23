package play.example.game.app.module.activity.base

import play.example.game.app.module.activity.base.entity.ActivityEntity
import play.example.game.app.module.activity.base.entity.PlayerActivityEntity
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.player.PlayerManager
import play.example.game.app.module.player.event.PlayerEventBus

/**
 *
 * @author LiangZengle
 */
interface BalancableActivityHandler {

  val playerEventBus: PlayerEventBus

  fun join(
    self: PlayerManager.Self,
    playerActivityEntity: PlayerActivityEntity,
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    activityEntity.join(self.id)
  }

  fun balance(
    activityEntity: ActivityEntity,
    resource: ActivityResource
  ) {
    for (playerId in activityEntity.getJoinedPlayers()) {
//      playerEventBus.publish()
    }
  }
}
