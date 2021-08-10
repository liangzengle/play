package play.entity

/**
 * Complex Primary Key
 * @author LiangZengle
 */
abstract class ObjId {

  abstract override fun toString(): String

  abstract override fun hashCode(): Int

  abstract override fun equals(other: Any?): Boolean
}
