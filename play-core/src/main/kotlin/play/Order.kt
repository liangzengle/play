package play

import java.lang.reflect.AnnotatedElement

/**
 * General Purpose Order Annotation
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Order(val value: Int) {
  companion object {
    const val Highest = Int.MIN_VALUE
    const val Lowest = Int.MAX_VALUE
    const val Normal = 0

    fun getOrder(elem: AnnotatedElement): Int {
      return elem.getAnnotation(Order::class.java)?.value ?: Normal
    }
  }
}
