package play.util.ranking

import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
enum class TestRankingType : RankingType<Any> {

  Score {
    override fun spec(): RankingListSpec<Any> {
      return ScoreRankingListSpec.unsafeCast()
    }
  },
}
