@file:JvmName("Orders")

package play

import com.google.common.collect.ComparisonChain
import play.util.reflect.Reflect
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 *
 * @author LiangZengle
 */
object Orders {
  const val Highest = Int.MIN_VALUE
  const val Lowest = Int.MAX_VALUE
  const val Normal = 0

  @JvmStatic
  private val ByClassOrderComparator = Comparator<Any> { o1, o2 ->
    if (o1 === o2) 0
    else {
      ClassOrderComparator.compare(o1.javaClass, o2.javaClass)
    }
  }

  @JvmStatic
  private val ClassOrderComparator = Comparator<Class<*>> { o1, o2 ->
    if (o1 === o2) 0
    else {
      ComparisonChain.start()
        .compare(getOrder(o1), getOrder(o2))
        .compare(o1.name, o2.name)
        .result()
    }
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  fun <T : Any> byClassOrderComparator(): Comparator<T> {
    return ByClassOrderComparator as Comparator<T>
  }

  @JvmStatic
  fun <T : Any> byFunctionThenClassOrderComparator(functionDeclaration: KFunction<*>): Comparator<T> {
    return byMethodThenClassOrderComparator(functionDeclaration.javaMethod!!)
  }

  @JvmStatic
  fun <T : Any> byMethodThenClassOrderComparator(method: Method): Comparator<T> {
    return byMethodThenClassOrderComparator(method.name, *method.parameterTypes)
  }

  @JvmStatic
  fun <T : Any> byMethodThenClassOrderComparator(methodName: String, vararg parameterTypes: Class<*>): Comparator<T> {
    return Comparator { o1, o2 ->
      val clazz1 = o1.javaClass
      val clazz2 = o2.javaClass
      val method1 = Reflect.findDeclaredMethod(clazz1, methodName, *parameterTypes)
      val method2 = Reflect.findDeclaredMethod(clazz2, methodName, *parameterTypes)
      val method1Order = if (method1 == null) 0 else getOrder(method1)
      val method2Order = if (method2 == null) 0 else getOrder(method2)
      var r = method1Order.compareTo(method2Order)
      if (r == 0) {
        r = ClassOrderComparator.compare(clazz1, clazz2)
      }
      r
    }
  }

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
