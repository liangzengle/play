package play.mvc

abstract class RequestCommander {

  abstract val id: Long

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    return id == (other as RequestCommander).id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "${javaClass.simpleName}($id)"
}
