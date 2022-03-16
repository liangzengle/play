package play.util.ranking

import play.util.ranking.primitive.SimpleRankingElementLong

/**
 *
 * @author LiangZengle
 */
object ScoreRankingListSpecification : RankingListSpecification<SimpleRankingElementLong> {
  override fun elementType(): Class<SimpleRankingElementLong> {
    return SimpleRankingElementLong::class.java
  }

  override fun comparator(): Comparator<SimpleRankingElementLong> {
    return SimpleRankingElement.comparatorDesc()
  }

  override fun maxRankingSize(): Int {
    return 10
  }

  override fun rankRequirement() = ScoreRankingRequirement
}
