package play.util.ranking

/**
 *
 * @author LiangZengle
 */
abstract class RankingElementLong : RankingElement<Long>() {

  abstract val id: Long

  @Deprecated(message = "use id property to avoid auto boxing/unboxing", replaceWith = ReplaceWith("id"))
  override fun id(): Long {
    return id
  }

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

