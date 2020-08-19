package play.util.ranking

/**
 *
 * @author LiangZengle
 */
interface SimpleRankingElement {
  val value: Long

  @Suppress("UNCHECKED_CAST")
  companion object {
    private val COMPARATOR_ASC = Comparator<SimpleRankingElement> { o1, o2 -> o1.value.compareTo(o2.value) }

    private val COMPARATOR_DESC = Comparator<SimpleRankingElement> { o1, o2 -> o2.value.compareTo(o1.value) }

    fun <T : SimpleRankingElement> comparatorAsc() = COMPARATOR_ASC as Comparator<T>

    fun <T : SimpleRankingElement> comparatorDesc() = COMPARATOR_DESC as Comparator<T>
  }
}
