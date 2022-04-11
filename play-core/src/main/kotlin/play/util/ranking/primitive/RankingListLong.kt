package play.util.ranking.primitive

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap
import play.util.json.Json
import play.util.ranking.RankingHelper
import play.util.ranking.RankingList
import play.util.ranking.RankingType
import play.util.unsafeCast
import java.util.*

/**
 *
 * @author LiangZengle
 */
class RankingListLong<E : RankingElementLong>(
  val rankingType: RankingType<E>, private val elements: NavigableSet<E>
) {

  @Transient
  private var elementRankUpdated = false

  @Transient
  private val map = LongObjectHashMap<E>()

  private val specification get() = rankingType.specification()

  constructor(rankingType: RankingType<E>) : this(
    rankingType, TreeSet(rankingType.specification().comparator())
  )

  init {
    for (element in elements) {
      map.put(element.id, element)
    }
  }

  fun insertOrUpdate(element: E) {
    elementRankUpdated = false
    val prev = map.remove(element.id)
    if (prev != null) {
      elements.remove(prev)
    }
    elements.add(element)
    map.put(element.id, element)

    val maxRankingSize = specification.maxRankingSize()
    while (maxRankingSize > 0 && elements.size > maxRankingSize) {
      val last = elements.pollLast()
      if (last != null) {
        map.remove(last.id)
      }
    }
  }

  fun update(element: E) {
    if (map.get(element.id) != null) {
      insertOrUpdate(element)
    }
  }

  fun getRankById(id: Long): Int {
    updateElementsRank()
    return map.get(id)?._rank ?: 0
  }

  private fun updateElementsRank() {
    if (!elementRankUpdated) {
      RankingHelper.updateElementsRank(rankingType, elements) { element, rank -> element._rank = rank }
      elementRankUpdated = true
    }
  }

  fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T> {
    updateElementsRank()
    return RankingHelper.subRank<E, T>(elements, fromRankInclusive, toRankInclusive, { it._rank }, transformer)
  }

  fun toGeneric(): RankingList<Long, RankingElementLongAdapter> {
    return RankingListLongAdapter(this.unsafeCast())
  }

  fun <T> toList(transformer: (Int, E) -> T): List<T> {
    updateElementsRank()
    return elements.asSequence().map { transformer(it._rank, it) }.toList()
  }

  companion object {
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): RankingListLong<RankingElementLong> {
      val rankTypeNode = node.get("rankType")
      val rankingType = Json.convert(rankTypeNode, jacksonTypeRef<RankingType<RankingElementLong>>())
      val elementsNode = node.get("elements")
      val specification = rankingType.specification()
      val elementList = Json.convert<List<RankingElementLong>>(
        elementsNode, Json.typeFactory().constructCollectionType(List::class.java, specification.elementType())
      )
      val elements = TreeSet(specification.comparator())
      elements.addAll(elementList)
      return RankingListLong(rankingType, elements)
    }
  }
}
