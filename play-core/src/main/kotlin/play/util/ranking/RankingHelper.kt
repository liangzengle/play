package play.util.ranking

import play.util.min

/**
 *
 * @author LiangZengle
 */
object RankingHelper {
  internal fun <E> updateElementsRank(
    rankingType: RankingType<E>, elements: Collection<E>, updater: (E, Int) -> Unit
  ) {
    var idx = 0
    var padding = 0
    val it = elements.iterator()
    val rankRequirement = rankingType.specification().rankRequirement()
    while (it.hasNext()) {
      val element = it.next()
      var rank = idx + padding + 1
      while (rankRequirement != null && !rankRequirement.isSatisfied(rank, element)) {
        rank++
        padding++
      }
      updater(element, rank)
      idx++
    }
  }

  internal fun <E, T> subRank(
    elements: Collection<E>,
    fromRankInclusive: Int,
    toRankInclusive: Int,
    rankAccessor: (E) -> Int,
    transformer: (Int, E) -> T
  ): List<T> {
    val result = ArrayList<T>(elements.size min (toRankInclusive - fromRankInclusive + 1))
    for (element in elements) {
      val rank = rankAccessor(element)
      if (rank in fromRankInclusive..toRankInclusive) {
        result.add(transformer(rank, element))
      }
    }
    return result
  }
}
