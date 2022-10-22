package play.util.ranking

data class SimpleRankingElementLong(override val id: Long, val value: Long) : RankingElementLong(),
  Comparable<SimpleRankingElementLong> {
  override fun compareTo(other: SimpleRankingElementLong): Int {
    return other.value.compareTo(this.value)
  }

  operator fun plus(that: SimpleRankingElementLong): SimpleRankingElementLong {
    return copy(value = this.value + that.value)
  }
}
