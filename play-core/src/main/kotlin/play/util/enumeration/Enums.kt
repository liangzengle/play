package play.util.enumeration

import java.util.*

/**
 *
 * @author LiangZengle
 */
object Enums {

  fun <T : Enum<T>> iterator(enumClass: Class<T>): Iterator<T> {
    return EnumSet.allOf(enumClass).iterator()
  }
}
