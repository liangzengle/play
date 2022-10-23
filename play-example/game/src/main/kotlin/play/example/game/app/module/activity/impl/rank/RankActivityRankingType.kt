package play.example.game.app.module.activity.impl.rank

import play.example.game.app.module.activity.impl.rank.res.RankActivitySettingConf
import play.util.classOf
import play.util.ranking.RankRequirement
import play.util.ranking.RankingListSpec
import play.util.ranking.RankingType
import play.util.ranking.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
object RankActivityRankingType : RankingType<SimpleRankingElementLong>,
  RankingListSpec<SimpleRankingElementLong> {

  override fun spec(): RankingListSpec<SimpleRankingElementLong> {
    return this
  }

  override fun elementType(): Class<SimpleRankingElementLong> = classOf()

  override fun maxRankingSize(): Int {
    return RankActivitySettingConf.rankSize
  }

  override fun rankRequirement(): RankRequirement<SimpleRankingElementLong> {
    return RankActivitySettingConf.get()
  }

  override fun compare(o1: SimpleRankingElementLong, o2: SimpleRankingElementLong): Int {
    return o1.compareTo(o2)
  }

  override fun update(old: SimpleRankingElementLong, change: SimpleRankingElementLong): SimpleRankingElementLong {
    return old + change
  }
}
