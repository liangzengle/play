package play.util.ranking

import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps
import play.util.min
import java.util.function.ToIntFunction

/**
 *
 * @author LiangZengle
 */
internal object RankingHelper {
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
      if (rank < fromRankInclusive) continue
      if (rank > toRankInclusive) break
      result.add(transformer(rank, element))
    }
    return result
  }

  internal fun <E> toRankMap(elements: Collection<E>, rankMapper: ToIntFunction<E>): IntObjectMap<E> {
    val result = IntObjectMaps.mutable.withInitialCapacity<E>(elements.size)
    for (element in elements) {
      result.put(rankMapper.applyAsInt(element), element)
    }
    return result
  }
}
