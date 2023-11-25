package play.ranking

/**
 *
 * @author LiangZengle
 */
abstract class RankingElement<ID : Comparable<ID>> {

  @Transient
  internal var _rank = 0

  /** 变化时间 */
  internal var time = 0L

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
