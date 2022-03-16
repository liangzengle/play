package play.util.ranking

import play.util.ranking.primitive.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
object ScoreRankingRequirement : RankRequirement<SimpleRankingElementLong> {
  override fun isSatisfied(rank: Int, element: SimpleRankingElementLong): Boolean {
    return when (rank) {
      1 -> element.value >= 100
      2 -> element.value >= 90
      3 -> element.value >= 80
      else -> true
    }
  }
}
