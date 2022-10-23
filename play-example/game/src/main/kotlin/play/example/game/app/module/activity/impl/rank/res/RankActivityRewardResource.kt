package play.example.game.app.module.activity.impl.rank.res

import jakarta.validation.constraints.Positive
import play.example.game.app.module.reward.model.RewardList
import play.res.AbstractResource
import play.res.validation.constraints.ReferTo

/**
 * 排名奖励
 * @author LiangZengle
 */
class RankActivityRewardResource : AbstractResource() {

  override val id: Int = 0

  @ReferTo(RankActivityResource::class)
  @Positive
  val activityId = 0

  val rankRange = IntRange.EMPTY

  val rewards = RewardList.Empty
}
