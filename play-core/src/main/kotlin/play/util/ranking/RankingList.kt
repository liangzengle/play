package play.util.ranking

/**
 *
 * @author LiangZengle
 */
abstract class RankingList<ID, E : RankingElement<ID>>(val rankingType: RankingType<E>) {

  abstract fun insertOrUpdate(element: E)

  abstract fun update(element: E)

  abstract fun getRankById(id: ID): Int

  abstract fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T>

  abstract fun <T> toList(transformer: (Int, E) -> T): List<T>
}
