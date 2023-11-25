package play.util.reflect

import play.util.EmptyObjectArray
import java.lang.reflect.Method

/**
 *
 * @author LiangZengle
 */
class NoArgMethodInvoker(val method: Method, val target: Any?) {
  init {
    if (method.parameterCount != 0) {
      throw IllegalArgumentException("${method}不是无参方法")
    }
    method.trySetAccessible()
  }

  fun invoke() {
    method.invoke(target, EmptyObjectArray)
  }

  fun invoke(target: Any?) {
    method.invoke(target, EmptyObjectArray)
  }

  override fun toString(): String {
    return (target?.javaClass ?: method.declaringClass).simpleName + '.' + method.name
  }
}
