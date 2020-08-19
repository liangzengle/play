package play.db

sealed class Entity<ID : Any> {
  abstract fun id(): ID

  /**
   * 从数据库加载出来后的操作
   */
  open fun postLoad() {}

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Entity<*>
    if (id() != other.id()) return false
    return true
  }

  override fun hashCode(): Int {
    return id().hashCode()
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(${id()})"
  }
}

abstract class EntityLong(val id: Long) : Entity<Long>() {

  @Deprecated(message = "Use getId to avoid auto boxing/unboxing", replaceWith = ReplaceWith("id"))
  override fun id(): Long = id

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EntityLong

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }


}

abstract class EntityInt(val id: Int) : Entity<Int>() {
  @Deprecated(message = "Use getId() to avoid auto boxing/unboxing", replaceWith = ReplaceWith("id"))
  override fun id(): Int = id

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EntityInt

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id
  }
}

abstract class EntityString(val id: String) : Entity<String>() {
  override fun id(): String = id
}
