package play.util.ranking.primitive

import play.util.ranking.SimpleRankingElement

/**
 *
 * @author LiangZengle
 */
abstract class RankingElementLong(@JvmField val id: Long) {

  @Transient
  internal var _rank = 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RankingElementLong

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  abstract override fun toString(): String
}

class SimpleRankingElementLong(id: Long, override val value: Long) : RankingElementLong(id), SimpleRankingElement {

  override fun toString(): String {
    return "SimpleRankingElementLong(id=$id, value=$value)"
  }
}
