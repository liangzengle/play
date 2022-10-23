package play.example.game.app.module.activity.impl.rank.res

import jakarta.validation.constraints.Positive
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.player.event.PlayerEvent
import play.res.AbstractResource
import play.res.validation.constraints.ReferTo

/**
 *
 * @author LiangZengle
 */
class RankActivityResource(val rankType: ActivityRankType<PlayerEvent>) : AbstractResource() {

  @ReferTo(ActivityResource::class)
  @Positive
  override val id: Int = 0
}
