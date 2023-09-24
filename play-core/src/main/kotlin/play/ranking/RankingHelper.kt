package play.ranking

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
    val rankRequirement = rankingType.spec().rankRequirement()
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

  @Suppress("UNCHECKED_CAST")
  internal fun <E : RankingElement<*>> toComparator(spec: RankingListSpec<E>): Comparator<E> {
    return Comparator { o1, o2 ->
      if (o1.id() == o2.id()) {
        0
      } else {
        var r = spec.compare(o1, o2)
        if (r == 0) {
          r = o2.time.compareTo(o1.time)
        }
        if (r == 0) {
          val id1 = o1.id() as Comparable<Comparable<*>>
          val id2 = o2.id() as Comparable<Comparable<*>>
          r = id1.compareTo(id2)
        }
        r
      }
    }
  }

  internal fun <E : RankingElementLong> toComparatorLong(spec: RankingListSpec<E>): Comparator<E> {
    return Comparator { o1, o2 ->
      if (o1.id == o2.id) {
        0
      } else {
        var r = spec.compare(o1, o2)
        if (r == 0) {
          r = o2.time.compareTo(o1.time)
        }
        if (r == 0) {
          r = o1.id.compareTo(o2.id)
        }
        r
      }
    }
  }
}
