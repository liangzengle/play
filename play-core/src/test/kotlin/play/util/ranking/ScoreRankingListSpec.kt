package play.util.ranking

/**
 *
 * @author LiangZengle
 */
object ScoreRankingListSpec : RankingListSpec<SimpleRankingElementLong>, RankRequirement<SimpleRankingElementLong> {
  override fun elementType(): Class<SimpleRankingElementLong> {
    return SimpleRankingElementLong::class.java
  }

  override fun compare(o1: SimpleRankingElementLong, o2: SimpleRankingElementLong): Int {
    return o1.compareTo(o2)
  }

  override fun maxRankingSize(): Int {
    return 10
  }

  override fun rankRequirement() = this

  override fun update(old: SimpleRankingElementLong, change: SimpleRankingElementLong): SimpleRankingElementLong {
    return old.copy(value = old.value + change.value)
  }

  override fun isSatisfied(rank: Int, element: SimpleRankingElementLong): Boolean {
    return when (rank) {
      1 -> element.value >= 100
      2 -> element.value >= 90
      3 -> element.value >= 80
      else -> true
    }
  }
}
