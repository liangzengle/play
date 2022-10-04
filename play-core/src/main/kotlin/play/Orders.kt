@file:JvmName("Orders")

package play

import java.lang.reflect.AnnotatedElement

/**
 *
 * @author LiangZengle
 */
object Orders {
  const val Highest = Int.MIN_VALUE
  const val Lowest = Int.MAX_VALUE
  const val Normal = 0

  val comparator = Comparator.comparingInt<Any> { getOrder(it.javaClass) }

  @JvmStatic
  fun getOrder(elem: AnnotatedElement): Int {
    return elem.getAnnotation(Order::class.java)?.value ?: Normal
  }

  @JvmStatic
  fun lowerThan(priority: Int): Int {
    require(priority < Lowest) { "can not lower than $priority" }
    return priority + 1
  }

  @JvmStatic
  fun higherThan(priority: Int): Int {
    require(priority > Highest) { "can not higher than $priority" }
    return priority - 1
  }
}
