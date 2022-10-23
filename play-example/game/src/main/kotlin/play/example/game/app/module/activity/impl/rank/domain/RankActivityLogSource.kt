package play.example.game.app.module.activity.impl.rank.domain

import play.example.common.LogSource
import play.example.common.ModularCode
import play.example.game.app.module.ModuleId

/**
 *
 * @author LiangZengle
 */
@ModularCode
object RankActivityLogSource : LogSource(ModuleId.ActivityRank) {

  /** 排名奖励 */
  val Reward = 1
}
