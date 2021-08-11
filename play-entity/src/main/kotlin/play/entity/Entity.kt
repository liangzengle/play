package play.entity

sealed class Entity<ID : Any> {

  private var deleted: Boolean? = null

  abstract fun id(): ID

  /**
   * 初始化
   */
  open fun initialize() {}

  internal fun delete() {
    deleted = java.lang.Boolean.TRUE
  }

  internal fun isDeleted(): Boolean {
    return deleted == java.lang.Boolean.TRUE
  }

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
    return if (!isDeleted()) {
      "${javaClass.simpleName}(${id()})"
    } else {
      "${javaClass.simpleName}(${id()}) <deleted>"
    }
  }
}

abstract class LongIdEntity(val id: Long) : Entity<Long>() {

  @Deprecated(message = "Use id to avoid auto boxing/unboxing", replaceWith = ReplaceWith("id"))
  override fun id(): Long = id

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LongIdEntity

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

abstract class IntIdEntity(val id: Int) : Entity<Int>() {
  @Deprecated(message = "Use id to avoid auto boxing/unboxing", replaceWith = ReplaceWith("id"))
  override fun id(): Int = id

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IntIdEntity

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id
  }
}

abstract class ObjIdEntity<ID : ObjId>(val id: ID) : Entity<ID>() {
  override fun id(): ID = id
}
