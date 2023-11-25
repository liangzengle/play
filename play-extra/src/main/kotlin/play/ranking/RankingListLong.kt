package play.ranking

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.eclipse.collections.api.map.primitive.IntObjectMap
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap
import play.util.json.Json
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
class RankingListLong<E : RankingElementLong>(
  override val rankingType: RankingType<E>, _elements: Collection<E>
) : IRankingList<Long, E> {

  @Transient
  @Volatile
  private var changed = false

  /** 全部数据 */
  @Transient
  private val map = LongObjectHashMap<E>()

  /** 排行榜上的数据 */
  @Transient
  private val rankingElements: NavigableSet<E> = TreeSet(RankingHelper.toComparatorLong(rankingType.spec()))

  @Transient
  private val wlLock = ReentrantReadWriteLock()

  private val spec get() = rankingType.spec()

  constructor(rankingType: RankingType<E>) : this(rankingType, emptyList())

  init {
    for (element in _elements) {
      update(element)
    }
    changed = !map.isEmpty
  }

  override fun update(element: E) {
    update(element, true)
  }

  override fun updateIfExists(element: E) {
    update(element, false)
  }

  override fun getRankById(id: Long): Int {
    return read {
      map.get(id)?._rank ?: 0
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
      val prev = map.get(element.id)
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
      map.put(element.id, theElement)

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
  internal fun elements() = wlLock.read { map.values().toList() }

  companion object {
    @JvmStatic
    @JsonCreator
    private fun fromJson(node: ObjectNode): RankingListLong<RankingElementLong> {
      val rankTypeNode = node.get("rankingType")
      val rankingType = Json.convert(rankTypeNode, jacksonTypeRef<RankingType<RankingElementLong>>())
      val elementsNode = node.get("elements")
      val spec = rankingType.spec()
      val elementList = Json.convert<List<RankingElementLong>>(
        elementsNode, Json.typeFactory().constructCollectionType(List::class.java, spec.elementType())
      )
      val elements = TreeSet(RankingHelper.toComparatorLong(spec))
      elements.addAll(elementList)
      return RankingListLong(rankingType, elements)
    }
  }
}
