package play.util.ranking

import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
enum class TestRankingType : RankingType<Any> {

  Score {
    override fun specification(): RankingListSpecification<Any> {
      return ScoreRankingListSpecification.unsafeCast()
    }
  },
}
