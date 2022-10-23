package play.example.game.app.module.activity.base.entity

import play.example.game.app.module.activity.impl.rank.RankActivityRankingType
import play.util.collection.ConcurrentHashSetLong
import play.util.collection.SerializableAttributeKey
import play.util.ranking.RankingListLong
import play.util.ranking.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
object ActivityDataKey {

  @JvmStatic
  val None = SerializableAttributeKey.valueOf<Int>("none")

  @JvmStatic
  val Test = SerializableAttributeKey.valueOf<Int>("Test")

  @JvmStatic
  val Login = SerializableAttributeKey.valueOf<Int>("Login")

  @JvmStatic
  val JoinedPlayers = SerializableAttributeKey.valueOf<ConcurrentHashSetLong>("JoinedPlayers")

  @JvmStatic
  val Rank = SerializableAttributeKey.valueOf<RankingListLong<SimpleRankingElementLong>>("Rank")
  fun getOrCreateRank(entity: ActivityEntity): RankingListLong<SimpleRankingElementLong> {
    return entity.data.attr(Rank).computeIfAbsent { RankingListLong(RankActivityRankingType) }
  }
}
