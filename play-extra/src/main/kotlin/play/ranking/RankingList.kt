package play.ranking

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.collections.api.map.primitive.IntObjectMap
import play.util.json.Json
import play.util.unsafeCast
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.ThreadSafe
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 *
 * @author LiangZengle
 */
@ThreadSafe
class RankingList<ID : Comparable<ID>, E : RankingElement<ID>>(
  override val rankingType: RankingType<E>,
  private val _elements: Collection<E>
) : IRankingList<ID, E> {

  @Transient
  @Volatile
  private var changed = false

  @Transient
  private val map = hashMapOf<ID, E>()

  @Transient
  private val rankingElements: NavigableSet<E> = TreeSet(RankingHelper.toComparator(rankingType.spec()))

  @Transient
  private val wlLock = ReentrantReadWriteLock()

  private val spec get() = rankingType.spec()

  constructor(rankingType: RankingType<E>) : this(rankingType, emptyList())

  init {
    for (element in rankingElements) {
      update(element)
    }
    changed = map.isNotEmpty()
  }

  override fun update(element: E) {
    update(element, true)
  }

  override fun updateIfExists(element: E) {
    update(element, false)
  }

  override fun getRankById(id: ID): Int {
    return read {
      map[id]?._rank ?: 0
    }
  }

  override fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T> {
    return read {
      RankingHelper.subRank<E, T>(rankingElements, fromRankInclusive, toRankInclusive, { it._rank }, transformer)
    }
  }

  override fun <T> toList(transformer: (Int, E) -> T): List<T> {
    return read {
      rankingElements.asSequence().map { transformer(it._rank, it) }.toList()
    }
  }

  override fun toRankMap(): IntObjectMap<E> {
    return read {
      RankingHelper.toRankMap(rankingElements) { it._rank }
    }
  }

  private fun update(element: E, upsert: Boolean) {
    wlLock.write {
      val prev = map[element.id()]
      if (prev == null && !upsert) {
        return
      }
      changed = true
      val theElement: E
      if (prev != null) {
        theElement = spec.update(prev, element)
        rankingElements.remove(prev)
      } else {
        theElement = element
      }
      rankingElements.add(theElement)
      map[element.id()] = theElement

      val maxRankingSize = spec.maxRankingSize()
      while (maxRankingSize > 0 && rankingElements.size > maxRankingSize) {
        val e = rankingElements.pollLast()
        if (e != null) {
          e._rank = 0
        }
      }
    }
  }

  private fun <T> read(action: () -> T): T {
    while (true) {
      if (!changed && wlLock.readLock().tryLock()) {
        try {
          return action()
        } finally {
          wlLock.readLock().unlock()
        }
      } else {
        wlLock.write {
          if (changed) {
            RankingHelper.updateElementsRank(rankingType, rankingElements) { element, rank -> element._rank = rank }
            changed = false
          }
          return action()
        }
      }
    }
  }

  @JsonProperty("elements")
  internal fun elements() = wlLock.read { map.values.toList() }

  companion object {
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): RankingList<*, RankingElement<*>> {
      val rankTypeNode = node.get("rankingType")
      val rankingType =
        Json.convert(rankTypeNode, RankingType::class.java) as RankingType<RankingElement<Comparable<Comparable<*>>>>
      val elementsNode = node.get("elements")
      val spec = rankingType.spec()
      val elementList = Json.convert<List<RankingElement<Comparable<Comparable<*>>>>>(
        elementsNode, Json.typeFactory().constructCollectionType(List::class.java, spec.elementType())
      )
      val elements: TreeSet<RankingElement<Comparable<Comparable<*>>>> = TreeSet(RankingHelper.toComparator(spec))
      elements.addAll(elementList)
      return RankingList(rankingType, elements).unsafeCast()
    }
  }
}
