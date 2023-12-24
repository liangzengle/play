package play.util

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Member
import java.lang.reflect.Parameter

object AnnotationUtil {

  fun <A : Annotation> getRequired(element: AnnotatedElement, annotationType: Class<A>): A {
    return element.getAnnotation(annotationType)
      ?: throw NoSuchElementException("Required annotation `${annotationType.name}` not found on `${getName(element)}`")
  }

  private fun getName(element: AnnotatedElement): String {
    return when (element) {
      is Class<*> -> element.name
      is Member -> element.name
      is Parameter -> element.name
      else -> element.toString()
    }
  }
}
