package play.util.ranking

/**
 *
 * @author LiangZengle
 */
abstract class RankingElement<ID> {

  @Transient
  internal var _rank = 0

  abstract fun id(): ID

  override fun hashCode(): Int {
    return id().hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as RankingElement<*>
    return this.id() == other.id()
  }

  override fun toString(): String {
    return "${this.javaClass.simpleName}(${id()})"
  }
}

abstract class SimpleRankingElementRef<ID>(
  val id: ID,
  override val value: Long
) : RankingElement<ID>(), SimpleRankingElement {
  override fun id(): ID {
    return id
  }
}
