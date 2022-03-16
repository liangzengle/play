package play.util.ranking.primitive

import play.util.ranking.*

class RankingElementLongAdapter(val underlying: RankingElementLong) : RankingElement<Long>() {
  override fun id(): Long = underlying.id
}

internal class RankingTypeAdapter(private val underlying: RankingType<RankingElementLong>) :
  RankingType<RankingElementLongAdapter> {
  private val specification = RankingListSpecificationAdapter(underlying.specification())

  override fun specification(): RankingListSpecification<RankingElementLongAdapter> {
    return specification
  }
}

internal class RankingListSpecificationAdapter(private val underlying: RankingListSpecification<RankingElementLong>) :
  RankingListSpecification<RankingElementLongAdapter> {
  override fun elementType(): Class<RankingElementLongAdapter> = throw UnsupportedOperationException()

  override fun comparator(): Comparator<RankingElementLongAdapter> {
    return Comparator { o1, o2 -> underlying.comparator().compare(o1.underlying, o2.underlying) }
  }

  override fun maxRankingSize(): Int = underlying.maxRankingSize()

  override fun rankRequirement(): RankRequirement<RankingElementLongAdapter>? {
    val rankRequirement = underlying.rankRequirement() ?: return null
    return RankRequirement { rank, element -> rankRequirement.isSatisfied(rank, element.underlying) }
  }
}

internal class RankingListLongAdapter(private val underlying: RankingListLong<RankingElementLong>) :
  RankingList<Long, RankingElementLongAdapter>(RankingTypeAdapter(underlying.rankingType)) {
  override fun insertOrUpdate(element: RankingElementLongAdapter) {
    underlying.insertOrUpdate(element.underlying)
  }

  override fun update(element: RankingElementLongAdapter) {
    underlying.update(element.underlying)
  }

  override fun getRankById(id: Long): Int {
    return underlying.getRankById(id)
  }

  override fun <T> subRank(
    fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, RankingElementLongAdapter) -> T
  ): List<T> {
    return underlying.subRank(fromRankInclusive, toRankInclusive) { rank, element ->
      transformer(rank, RankingElementLongAdapter(element))
    }
  }

  override fun <T> toList(transformer: (Int, RankingElementLongAdapter) -> T): List<T> {
    return underlying.toList { rank, element -> transformer(rank, RankingElementLongAdapter(element)) }
  }
}
