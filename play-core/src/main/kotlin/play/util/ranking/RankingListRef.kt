package play.util.ranking

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import play.util.json.Json
import play.util.unsafeCast
import java.util.*

/**
 *
 * @author LiangZengle
 */
class RankingListRef<ID, E : RankingElement<ID>>(rankingType: RankingType<E>, private val elements: NavigableSet<E>) :
  RankingList<ID, E>(rankingType) {

  @Transient
  private var elementRankUpdated = false

  @Transient
  private val map = hashMapOf<ID, E>()

  constructor(rankingType: RankingType<E>) : this(
    rankingType, TreeSet(rankingType.specification().comparator().unsafeCast<Comparator<RankingElement<*>>>())
  )

  init {
    for (element in elements) {
      map[element.id()] = element
    }
  }

  override fun insertOrUpdate(element: E) {
    elementRankUpdated = false
    val prev = map.remove(element.id())
    if (prev != null) {
      elements.remove(prev)
    }
    elements.add(element)
    map[element.id()] = element

    val maxRankingSize = rankingType.specification().maxRankingSize()
    while (maxRankingSize > 0 && elements.size > maxRankingSize) {
      val last = elements.pollLast()
      if (last != null) {
        map.remove(last.id())
      }
    }
  }

  override fun update(element: E) {
    if (map.containsKey(element.id())) {
      insertOrUpdate(element)
    }
  }

  override fun getRankById(id: ID): Int {
    updateElementsRank()
    return map[id]?._rank ?: 0
  }

  private fun updateElementsRank() {
    if (!elementRankUpdated) {
      RankingHelper.updateElementsRank(rankingType, elements) { element, rank -> element._rank = rank }
      elementRankUpdated = true
    }
  }

  override fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T> {
    updateElementsRank()
    return RankingHelper.subRank<E, T>(elements, fromRankInclusive, toRankInclusive, { it._rank }, transformer)
  }

  override fun <T> toList(transformer: (Int, E) -> T): List<T> {
    updateElementsRank()
    return elements.asSequence().map { transformer(it._rank, it) }.toList()
  }

  companion object {
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): RankingListRef<Any, RankingElement<Any>> {
      val rankTypeNode = node.get("rankType")
      val rankingType = Json.convert(rankTypeNode, jacksonTypeRef<RankingType<RankingElement<Any>>>())
      val elementsNode = node.get("elements")
      val specification = rankingType.specification()
      val elementList = Json.convert<List<RankingElement<Any>>>(
        elementsNode, Json.typeFactory().constructCollectionType(List::class.java, specification.elementType())
      )
      val elements = TreeSet(specification.comparator().unsafeCast<Comparator<RankingElement<Any>>>())
      elements.addAll(elementList)
      return RankingListRef(rankingType, elements)
    }
  }
}
