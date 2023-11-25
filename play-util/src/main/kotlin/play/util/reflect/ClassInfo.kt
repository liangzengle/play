package play.util.reflect

import io.github.classgraph.ClassInfo

fun ClassInfo.isAnnotationPresent(annotationType: Class<out Annotation>): Boolean {
  return hasAnnotation(annotationType.name)
}

inline fun <reified T : Annotation> ClassInfo.isAnnotationPresent(): Boolean = isAnnotationPresent(T::class.java)
